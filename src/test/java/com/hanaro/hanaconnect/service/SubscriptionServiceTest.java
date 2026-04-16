package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.dto.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.dto.SubscriptionRequestDto;
import com.hanaro.hanaconnect.dto.SubscriptionResponseDto;
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

	@Test
	@DisplayName("청약 진입 정보 조회 성공")
	void getSubscriptionPaymentInfo_success() {
		// given
		Member parent3 = findMemberByName("청약할머니");
		Account subscriptionAccount = findAccountByNumber("999900001111");

		// when
		SubscriptionInfoResponseDto result =
			subscriptionService.getSubscriptionPaymentInfo(parent3.getId(), subscriptionAccount.getId());

		// then
		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getAccountNumber()).isEqualTo("999900001111");
		assertThat(result.getDisplayName()).contains("김청약");
		assertThat(result.getAccountNickname()).isEqualTo("김청약 주택청약");
		assertThat(result.getRewardAccountName()).isEqualTo("청약할머니 리워드 통장");
		assertThat(result.getAlreadyPaidAmount()).isNotNull();
	}

	@Test
	@DisplayName("첫 청약 납입 성공 - 25만원 이하")
	void paySubscription_firstPayment_success() {
		// given
		Member parent1 = findMemberByName("김엄마");
		Account freeAccount = findAccountByNumber("22233335555");
		Account subscriptionAccount = findAccountByNumber("77788889999");

		BigDecimal beforeFreeBalance = freeAccount.getBalance();
		BigDecimal beforeSubscriptionBalance = subscriptionAccount.getBalance();

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("200000"));
		request.setPassword("123456");
		request.setPrepaymentCount(null);
		request.setTransferExcessToReward(null);

		// when
		SubscriptionResponseDto result =
			subscriptionService.paySubscription(parent1.getId(), subscriptionAccount.getId(), request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber()).isEqualTo("77788889999");
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("200000");
		assertThat(result.getRewardAmount()).isEqualByComparingTo("0");
		assertThat(result.getRewardAccountNumber()).isNull();
		assertThat(result.getPrepaymentCount()).isNull();

		assertThat(freeAccount.getBalance())
			.isEqualByComparingTo(beforeFreeBalance.subtract(new BigDecimal("200000")));
		assertThat(subscriptionAccount.getBalance())
			.isEqualByComparingTo(beforeSubscriptionBalance.add(new BigDecimal("200000")));
	}

	@Test
	@DisplayName("첫 청약 납입 성공 - 25만원 초과분 리워드 계좌 입금")
	void paySubscription_firstPayment_withReward_success() {
		// given
		Member parent1 = findMemberByName("김엄마");
		Account freeAccount = findAccountByNumber("22233335555");
		Account subscriptionAccount = findAccountByNumber("77788889999");
		Account rewardAccount = findAccountByNumber("22233336666");

		BigDecimal beforeFreeBalance = freeAccount.getBalance();
		BigDecimal beforeSubscriptionBalance = subscriptionAccount.getBalance();
		BigDecimal beforeRewardBalance = rewardAccount.getBalance();

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("300000"));
		request.setPassword("123456");
		request.setPrepaymentCount(null);
		request.setTransferExcessToReward(true);

		// when
		SubscriptionResponseDto result =
			subscriptionService.paySubscription(parent1.getId(), subscriptionAccount.getId(), request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber()).isEqualTo("77788889999");
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("250000");
		assertThat(result.getRewardAmount()).isEqualByComparingTo("50000");
		assertThat(result.getRewardAccountNumber()).isEqualTo("22233336666");
		assertThat(result.getPrepaymentCount()).isNull();

		assertThat(freeAccount.getBalance())
			.isEqualByComparingTo(beforeFreeBalance.subtract(new BigDecimal("300000")));
		assertThat(subscriptionAccount.getBalance())
			.isEqualByComparingTo(beforeSubscriptionBalance.add(new BigDecimal("250000")));
		assertThat(rewardAccount.getBalance())
			.isEqualByComparingTo(beforeRewardBalance.add(new BigDecimal("50000")));
	}

	@Test
	@DisplayName("이번 달 이미 납입한 뒤 선납 성공")
	void paySubscription_prepayment_success() {
		// given
		Member parent3 = findMemberByName("청약할머니");
		Account freeAccount = findAccountByNumber("777788889999");
		Account subscriptionAccount = findAccountByNumber("999900001111");

		BigDecimal beforeFreeBalance = freeAccount.getBalance();
		BigDecimal beforeSubscriptionBalance = subscriptionAccount.getBalance();

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("400000"));
		request.setPassword("123456");
		request.setPrepaymentCount(2);

		// when
		SubscriptionResponseDto result =
			subscriptionService.paySubscription(parent3.getId(), subscriptionAccount.getId(), request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("400000");
		assertThat(result.getPrepaymentCount()).isEqualTo(2);
		assertThat(result.getRewardAmount()).isEqualByComparingTo("0");
		assertThat(result.getRewardAccountNumber()).isNull();

		assertThat(freeAccount.getBalance())
			.isEqualByComparingTo(beforeFreeBalance.subtract(new BigDecimal("400000")));
		assertThat(subscriptionAccount.getBalance())
			.isEqualByComparingTo(beforeSubscriptionBalance.add(new BigDecimal("400000")));
	}

	@Test
	@DisplayName("비밀번호가 일치하지 않으면 예외 발생")
	void paySubscription_fail_invalidPassword() {
		// given
		Member parent1 = findMemberByName("김엄마");
		Account subscriptionAccount = findAccountByNumber("77788889999");

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("100000"));
		request.setPassword("999999");

		// when & then
		assertThatThrownBy(() ->
			subscriptionService.paySubscription(parent1.getId(), subscriptionAccount.getId(), request)
		)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("비밀번호가 일치하지 않습니다.");
	}

	@Test
	@DisplayName("25만원 초과인데 리워드 여부를 선택하지 않으면 예외 발생")
	void paySubscription_fail_overMaxWithoutRewardChoice() {
		// given
		Member parent1 = findMemberByName("김엄마");
		Account subscriptionAccount = findAccountByNumber("77788889999");

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("300000"));
		request.setPassword("123456");
		request.setPrepaymentCount(null);
		request.setTransferExcessToReward(null);

		// when & then
		assertThatThrownBy(() ->
			subscriptionService.paySubscription(parent1.getId(), subscriptionAccount.getId(), request)
		)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("25만 원 초과 시 리워드 계좌 입금 여부를 선택해주세요.");
	}

	@Test
	@DisplayName("최근 청약 납입 결과 조회 성공")
	void getSubscriptionPaymentResult_success() {
		// given
		Member parent3 = findMemberByName("청약할머니");
		Account subscriptionAccount = findAccountByNumber("999900001111");

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("300000"));
		request.setPassword("123456");
		request.setPrepaymentCount(1);

		subscriptionService.paySubscription(parent3.getId(), subscriptionAccount.getId(), request);

		// when
		SubscriptionResponseDto result =
			subscriptionService.getSubscriptionPaymentResult(parent3.getId(), subscriptionAccount.getId());

		// then
		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber()).isEqualTo("999900001111");
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("300000");
		assertThat(result.getPaidAt()).isNotNull();
	}

	private Member findMemberByName(String name) {
		return memberRepository.findAll().stream()
			.filter(member -> name.equals(member.getName()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트 회원을 찾을 수 없습니다. name=" + name));
	}

	private Account findAccountByNumber(String accountNumber) {
		return accountRepository.findAll().stream()
			.filter(account -> accountNumber.equals(account.getAccountNumber()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트 계좌를 찾을 수 없습니다. accountNumber=" + accountNumber));
	}
}
