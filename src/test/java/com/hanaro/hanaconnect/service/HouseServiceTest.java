package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.util.HouseLevelCalculator;
import com.hanaro.hanaconnect.dto.HouseStatusResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;

import jakarta.persistence.EntityNotFoundException;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class HouseServiceTest {

	@Autowired
	private HouseService houseService;

	@Autowired
	private MemberRepository memberRepository;

	private Member findKidWithoutHouse() {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals("홍길동"))
			.filter(member -> member.getMemberRole() == MemberRole.KID)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 아이 회원(홍길동)을 찾을 수 없습니다."));
	}

	private Member findKidWithHouse() {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals("김청약"))
			.filter(member -> member.getMemberRole() == MemberRole.KID)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 아이 회원(김청약)을 찾을 수 없습니다."));
	}

	private Member findRelatedParent() {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals("청약할머니"))
			.filter(member -> member.getMemberRole() == MemberRole.PARENT)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 조부모 회원을 찾을 수 없습니다."));
	}

	private Member findUnrelatedParent() {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals("김엄마"))
			.filter(member -> member.getMemberRole() == MemberRole.PARENT)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 조부모 회원(김엄마)을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("아이가 본인 청약 리포트를 조회하면 최근 납입자 기준 personalized 메시지를 반환한다")
	void getHouseStatusByKidTest() {
		Member kid = findKidWithHouse();

		HouseStatusResponseDTO result = houseService.getHouseStatus(kid.getId(), null);

		assertThat(result).isNotNull();
		assertThat(result.getMemberId()).isEqualTo(kid.getId());
		assertThat(result.getTotalCount()).isEqualTo(28);
		assertThat(result.getMonthlyPayment()).isEqualByComparingTo("200000");
		assertThat(result.getStartDate()).isNotNull();
		assertThat(result.getLevel())
			.isEqualTo(HouseLevelCalculator.calculateLevel(result.getTotalCount()));
		assertThat(result.getGauge())
			.isEqualTo(HouseLevelCalculator.calculateGauge(result.getTotalCount()));
		assertThat(result.getMessage()).contains("할머니");
	}

	@Test
	@DisplayName("조부모가 연결된 아이의 청약 리포트를 조회하면 기본 메시지를 반환한다")
	void getHouseStatusByParentTest() {
		Member parent = findRelatedParent();
		Member kid = findKidWithHouse();

		HouseStatusResponseDTO result = houseService.getHouseStatus(parent.getId(), kid.getId());

		assertThat(result).isNotNull();
		assertThat(result.getMemberId()).isEqualTo(kid.getId());
		assertThat(result.getTotalCount()).isEqualTo(28);
		assertThat(result.getMessage()).isNotNull();
		assertThat(result.getMessage()).doesNotContain("할머니");
	}

	@Test
	@DisplayName("청약이 없는 아이가 조회하면 level 0 상태를 반환한다")
	void getHouseStatusWithoutHouseTest() {
		Member kid = findKidWithoutHouse();

		HouseStatusResponseDTO result = houseService.getHouseStatus(kid.getId(), null);

		assertThat(result).isNotNull();
		assertThat(result.getMemberId()).isEqualTo(kid.getId());
		assertThat(result.getLevel()).isEqualTo(0);
		assertThat(result.getGauge()).isEqualTo(0);
		assertThat(result.getTotalCount()).isNull();
		assertThat(result.getMonthlyPayment()).isNull();
		assertThat(result.getStartDate()).isNull();
		assertThat(result.getMessage()).isNull();
	}

	@Test
	@DisplayName("조부모가 kidId 없이 요청하면 예외가 발생한다")
	void getHouseStatusWithoutKidIdByParentTest() {
		Member parent = findRelatedParent();

		assertThatThrownBy(() -> houseService.getHouseStatus(parent.getId(), null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("kidId는 필수입니다.");
	}

	@Test
	@DisplayName("조부모가 관계 없는 아이를 조회하면 접근 예외가 발생한다")
	void getHouseStatusByUnrelatedParentTest() {
		Member parent = findUnrelatedParent();
		Member kid = findKidWithHouse();

		assertThatThrownBy(() -> houseService.getHouseStatus(parent.getId(), kid.getId()))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("해당 아이의 정보에 접근할 수 없습니다.");
	}

	@Test
	@DisplayName("존재하지 않는 requesterId로 조회하면 예외가 발생한다")
	void getHouseStatusByInvalidRequesterTest() {
		assertThatThrownBy(() -> houseService.getHouseStatus(999999L, null))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("회원 정보를 찾을 수 없습니다.");
	}
}
