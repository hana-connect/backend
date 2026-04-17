package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.account.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.dto.account.WalletResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MemberServiceImplTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private MemberService memberService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccountCryptoService accountCryptoService;

	private Member kid;
	private Member parent1;
	private Member parent2;

	private static long virtualAccountSeq = 30000000000L;

	@BeforeEach
	void setUp() {
		kid = createMember(
			"홍길동",
			LocalDate.of(2010, 1, 2),
			MemberRole.KID
		);

		parent1 = createMember(
			"김엄마",
			LocalDate.of(1980, 5, 19),
			MemberRole.PARENT
		);

		parent2 = createMember(
			"이할머니",
			LocalDate.of(1952, 8, 31),
			MemberRole.PARENT
		);

		// InitLoader와 동일하게 양방향 관계 저장
		relationRepository.save(createRelation(kid, parent1));
		relationRepository.save(createRelation(kid, parent2));
		relationRepository.save(createRelation(parent1, kid));
		relationRepository.save(createRelation(parent2, kid));
	}

	private Member createMember(
		String name,
		LocalDate birthday,
		MemberRole memberRole
	) {
		String rawVirtualAccount = generateVirtualAccount();

		return memberRepository.save(
			Member.builder()
				.name(name)
				.password(passwordEncoder.encode("123456"))
				.birthday(birthday)
				.virtualAccount(accountCryptoService.encrypt(rawVirtualAccount))
				.walletMoney(BigDecimal.ZERO)
				.memberRole(memberRole)
				.role(Role.USER)
				.build()
		);
	}

	private Relation createRelation(Member member, Member connectMember) {
		return Relation.builder()
			.member(member)
			.connectMember(connectMember)
			.connectMemberRole(connectMember.getMemberRole())
			.build();
	}

	private String generateVirtualAccount() {
		return String.valueOf(virtualAccountSeq++);
	}

	@Test
	@DisplayName("내 지갑 조회 성공")
	void getMyWalletTest() {
		WalletResponseDTO dto = memberService.getMyWallet(kid.getId());

		assertThat(dto).isNotNull();
		assertThat(dto.getWalletMoney()).isEqualByComparingTo("0.00");
	}

	@Test
	@DisplayName("내 지갑 조회 실패 - 회원 없음")
	void getMyWalletFailTest() {
		assertThatThrownBy(() -> memberService.getMyWallet(999L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("회원이 존재하지 않습니다");
	}

	@Test
	@DisplayName("부모 목록 조회 성공")
	void getParentsTest() {
		List<ConnectMemberResponseDTO> result = memberService.getParents(kid.getId());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberName)
			.containsExactlyInAnyOrder("김엄마", "이할머니");

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberRole)
			.containsOnly(MemberRole.PARENT);
	}

	@Test
	@DisplayName("아이 목록 조회 성공")
	void getKidsTest() {
		List<ConnectMemberResponseDTO> result = memberService.getKids(parent1.getId());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberName)
			.containsExactly("홍길동");

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberRole)
			.containsOnly(MemberRole.KID);
	}

	@Test
	@DisplayName("다른 부모 목록 조회 성공 - 해당 아이의 부모가 조회")
	void getOtherParentsTest() {
		List<ConnectMemberResponseDTO> result = memberService.getOtherParents(parent1.getId(), kid.getId());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberName)
			.containsExactly("이할머니");

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberRole)
			.containsOnly(MemberRole.PARENT);
	}

	@Test
	@DisplayName("다른 부모 목록 조회 실패 - 해당 아이와 연결되지 않은 부모")
	void getOtherParentsFailTest() {
		Member unrelatedParent = createMember(
			"박엄마",
			LocalDate.of(1985, 1, 1),
			MemberRole.PARENT
		);

		assertThatThrownBy(() -> memberService.getOtherParents(unrelatedParent.getId(), kid.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 아이와 연결된 부모만 조회할 수 있습니다");
	}
}
