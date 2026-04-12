package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.dto.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.dto.WalletResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

// @ActiveProfiles("test") // test에서는 InitLoader 실행 안되게
@Disabled("로컬 환경에서만 OpenAI 실제 호출 테스트 실행")
@SpringBootTest
class MemberServiceImplTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long NOT_FOUND_MEMBER_ID = 999L;

	private static final Member MEMBER = Member.builder()
		.id(MEMBER_ID)
		.name("김꼬마")
		.password("encoded-password")
		.birthday(LocalDate.of(2010, 1, 2))
		.virtualAccount("encrypted-account")
		.walletMoney(new BigDecimal("50000"))
		.memberRole(MemberRole.KID)
		.role(Role.USER)
		.build();

	private static final List<ConnectMemberResponseDTO> PARENTS = List.of(
		new ConnectMemberResponseDTO(2L, "김엄마", "우리 엄마", MemberRole.PARENT),
		new ConnectMemberResponseDTO(3L, "이할머니", "외할머니", MemberRole.PARENT)
	);

	@MockitoBean
	MemberRepository memberRepository;

	@MockitoBean
	RelationRepository relationRepository;

	@Autowired
	MemberService memberService;

	@BeforeEach
	void setUp() {
		given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(MEMBER));
		given(memberRepository.findById(NOT_FOUND_MEMBER_ID)).willReturn(Optional.empty());
		given(relationRepository.findParents(MEMBER_ID)).willReturn(PARENTS);
	}

	@Test
	@DisplayName("내 지갑 조회 성공")
	void getMyWalletTest() {
		WalletResponseDTO dto = memberService.getMyWallet(MEMBER_ID);

		assertThat(dto).isNotNull();
		assertThat(dto.getWalletMoney()).isEqualByComparingTo("50000");
	}

	@Test
	@DisplayName("내 지갑 조회 실패 - 회원 없음")
	void getMyWalletFailTest() {
		assertThatThrownBy(() -> memberService.getMyWallet(NOT_FOUND_MEMBER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("회원이 존재하지 않습니다");
	}

	@Test
	@DisplayName("부모 목록 조회 성공")
	void getParentsTest() {
		List<ConnectMemberResponseDTO> result = memberService.getParents(MEMBER_ID);

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberId)
			.containsExactly(2L, 3L);

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberName)
			.containsExactly("김엄마", "이할머니");

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberPhoneName)
			.containsExactly("우리 엄마", "외할머니");

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberRole)
			.containsOnly(MemberRole.PARENT);
	}
}
