package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.dto.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.dto.WalletResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MemberServiceImplTest {

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	RelationRepository relationRepository;

	@Autowired
	MemberService memberService;

	private Member findParentByName(String name) {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals(name))
			.filter(member -> member.getMemberRole() == MemberRole.PARENT)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 부모 회원(" + name + ")을 찾을 수 없습니다."));
	}

	private Member findKidByName(String name) {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals(name))
			.filter(member -> member.getMemberRole() == MemberRole.KID)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 아이 회원(" + name + ")을 찾을 수 없습니다."));
	}


	@Test
	@DisplayName("내 지갑 조회 성공")
	void getMyWalletTest() {
		Member kid = findKidByName("홍길동");

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
		Member kid = findKidByName("홍길동");

		List<ConnectMemberResponseDTO> result = memberService.getParents(kid.getId());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberName)
			.containsExactly("김엄마", "이할머니");

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberRole)
			.containsOnly(MemberRole.PARENT);
	}

	@Test
	@DisplayName("아이 목록 조회 성공")
	void getKidsTest() {
		Member parent = findParentByName("김엄마");

		List<ConnectMemberResponseDTO> result = memberService.getKids(parent.getId());

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
		Member kid = findKidByName("홍길동");
		Member parent = findParentByName("김엄마");

		List<ConnectMemberResponseDTO> result = memberService.getOtherParents(parent.getId(), kid.getId());

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
		Member kid = findKidByName("홍길동");

		Member unrelatedParent = memberRepository.save(
			Member.builder()
				.name("박엄마")
				.password("123456")
				.birthday(LocalDate.of(1985, 1, 1))
				.virtualAccount("temp-account")
				.walletMoney(new BigDecimal("0"))
				.memberRole(MemberRole.PARENT)
				.role(Role.USER)
				.build()
		);

		assertThatThrownBy(() -> memberService.getOtherParents(unrelatedParent.getId(), kid.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 아이와 연결된 부모만 조회할 수 있습니다");
	}
}
