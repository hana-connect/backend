package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.dto.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class SubscriptionServiceTest {

	@Autowired
	private SubscriptionService subscriptionService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AccountRepository accountRepository;

	private Member findKid1() {
		return memberRepository.findAll().stream()
			.filter(member -> "홍길동".equals(member.getName()))
			.filter(member -> member.getMemberRole() == MemberRole.KID)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 회원 홍길동을 찾을 수 없습니다."));
	}

	private Member findKid2() {
		return memberRepository.findAll().stream()
			.filter(member -> "김청약".equals(member.getName()))
			.filter(member -> member.getMemberRole() == MemberRole.KID)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 회원 김청약을 찾을 수 없습니다."));
	}

	private Account findSubscriptionAccount(Long memberId) {
		return accountRepository.findAll().stream()
			.filter(account -> account.getMember().getId().equals(memberId))
			.filter(account -> account.getAccountType() == AccountType.SUBSCRIPTION)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 청약 계좌를 찾을 수 없습니다."));
	}

	private Account findFreeAccount(Long memberId) {
		return accountRepository.findAll().stream()
			.filter(account -> account.getMember().getId().equals(memberId))
			.filter(account -> account.getAccountType() == AccountType.FREE)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 입출금 계좌를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("청약 납입 정보 조회 성공 - 이번 달 납입 이력 없음")
	void getSubscriptionPaymentInfoSuccessNotPaidThisMonthTest() {
		Member kid1 = findKid1();
		Account subscriptionAccount = findSubscriptionAccount(kid1.getId());

		SubscriptionInfoResponseDto result =
			subscriptionService.getSubscriptionPaymentInfo(kid1.getId(), subscriptionAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getAccountNumber()).isEqualTo(subscriptionAccount.getAccountNumber());
		assertThat(result.isHasPaidThisMonth()).isFalse();
		assertThat(result.getAlreadyPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("청약 납입 정보 조회 성공 - 이번 달 납입 이력 있음")
	void getSubscriptionPaymentInfoSuccessHasPaidThisMonthTest() {
		Member kid2 = findKid2();
		Account subscriptionAccount = findSubscriptionAccount(kid2.getId());

		SubscriptionInfoResponseDto result =
			subscriptionService.getSubscriptionPaymentInfo(kid2.getId(), subscriptionAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getAccountNumber()).isEqualTo(subscriptionAccount.getAccountNumber());
		assertThat(result.isHasPaidThisMonth()).isTrue();
		assertThat(result.getAlreadyPaidAmount()).isGreaterThan(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("청약 납입 정보 조회 실패 - 청약 계좌가 아님")
	void getSubscriptionPaymentInfoFailNotSubscriptionAccountTest() {
		Member kid1 = findKid1();
		Account freeAccount = findFreeAccount(kid1.getId());

		assertThatThrownBy(() ->
			subscriptionService.getSubscriptionPaymentInfo(kid1.getId(), freeAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("청약 계좌가 아닙니다.");
	}

	@Test
	@DisplayName("청약 납입 정보 조회 실패 - 존재하지 않는 청약 계좌")
	void getSubscriptionPaymentInfoFailAccountNotFoundTest() {
		Member kid1 = findKid1();

		assertThatThrownBy(() ->
			subscriptionService.getSubscriptionPaymentInfo(kid1.getId(), 999999L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("청약 계좌를 찾을 수 없습니다.");
	}
}
