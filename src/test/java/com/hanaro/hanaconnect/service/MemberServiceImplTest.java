package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.MemberRole;
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

	private Member findKid() {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals("홍길동"))
			.filter(member -> member.getMemberRole() == MemberRole.KID)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 아이 회원(홍길동)을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("내 지갑 조회 성공")
	void getMyWalletTest() {
		Member kid = findKid();

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
		Member kid = findKid();

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
}
