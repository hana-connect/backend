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
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionRequestDto;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.PrepaymentDetailRepository;

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

	@Autowired
	private PrepaymentDetailRepository prepaymentDetailRepository;

	@Autowired
	private AccountCryptoService accountCryptoService;

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

	private Member findParent1() {
		return memberRepository.findAll().stream()
			.filter(member -> "김엄마".equals(member.getName()))
			.filter(member -> member.getMemberRole() == MemberRole.PARENT)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 회원 김엄마를 찾을 수 없습니다."));
	}

	private Member findParent3() {
		return memberRepository.findAll().stream()
			.filter(member -> "청약할머니".equals(member.getName()))
			.filter(member -> member.getMemberRole() == MemberRole.PARENT)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 회원 청약할머니를 찾을 수 없습니다."));
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
			.filter(account -> !Boolean.TRUE.equals(account.getIsReward()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 입출금 계좌를 찾을 수 없습니다."));
	}

	private Account findRewardAccount(Long memberId) {
		return accountRepository.findAll().stream()
			.filter(account -> account.getMember().getId().equals(memberId))
			.filter(Account::getIsReward)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 리워드 계좌를 찾을 수 없습니다."));
	}

	private SubscriptionRequestDto createRequest(
		BigDecimal amount,
		Integer prepaymentCount,
		String password,
		Boolean transferExcessToReward
	) {
		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(amount);
		request.setPrepaymentCount(prepaymentCount);
		request.setPassword(password);
		request.setTransferExcessToReward(transferExcessToReward);
		return request;
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
		Member parent3 = findParent3();
		Member kid2 = findKid2();
		Account subscriptionAccount = findSubscriptionAccount(kid2.getId());

		SubscriptionInfoResponseDto result =
			subscriptionService.getSubscriptionPaymentInfo(parent3.getId(), subscriptionAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getAccountNumber()).isEqualTo(subscriptionAccount.getAccountNumber());
		assertThat(result.isHasPaidThisMonth()).isTrue();
		assertThat(result.getAlreadyPaidAmount()).isGreaterThan(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("청약 진입 정보 조회 성공 - 표시 정보 포함")
	void getSubscriptionPaymentInfoSuccessWithDisplayFieldsTest() {
		Member parent3 = findParent3();
		Member kid2 = findKid2();
		Account subscriptionAccount = findSubscriptionAccount(kid2.getId());
		Account rewardAccount = findRewardAccount(parent3.getId());

		SubscriptionInfoResponseDto result =
			subscriptionService.getSubscriptionPaymentInfo(parent3.getId(), subscriptionAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getAccountNumber()).isEqualTo(subscriptionAccount.getAccountNumber());
		assertThat(result.getAlreadyPaidAmount()).isNotNull();

		// develop 쪽에서 추가된 검증
		assertThat(result.getDisplayName()).contains("김청약");
		assertThat(result.getAccountNickname()).isEqualTo(subscriptionAccount.getName());
		assertThat(result.getRewardAccountName()).isEqualTo(rewardAccount.getName());
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

	@Test
	@DisplayName("청약 첫 납입 성공")
	void paySubscription_firstPayment_success() {
		Member parent1 = findParent1();
		Member kid1 = findKid1();

		Account freeAccount = findFreeAccount(parent1.getId());
		Account subscriptionAccount = findSubscriptionAccount(kid1.getId());

		BigDecimal freeBefore = freeAccount.getBalance();
		BigDecimal subscriptionBefore = subscriptionAccount.getBalance();

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("100000"),
			null,
			"123456",
			null
		);

		SubscriptionResponseDto result = subscriptionService.paySubscription(
			parent1.getId(),
			subscriptionAccount.getId(),
			request
		);

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber()).isEqualTo(subscriptionAccount.getAccountNumber());
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("100000");
		assertThat(result.getRewardAmount()).isEqualByComparingTo("0");
		assertThat(result.getRewardAccountNumber()).isNull();
		assertThat(result.getPrepaymentCount()).isNull();

		Account updatedFree = accountRepository.findById(freeAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("입출금 계좌를 다시 찾을 수 없습니다."));
		Account updatedSubscription = accountRepository.findById(subscriptionAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("청약 계좌를 다시 찾을 수 없습니다."));

		assertThat(updatedFree.getBalance()).isEqualByComparingTo(freeBefore.subtract(new BigDecimal("100000")));
		assertThat(updatedSubscription.getBalance()).isEqualByComparingTo(subscriptionBefore.add(new BigDecimal("100000")));

		Integer maxRoundNo = prepaymentDetailRepository.findMaxRoundNoByAccountId(subscriptionAccount.getId())
			.orElse(0);
		assertThat(maxRoundNo).isEqualTo(1);
	}

	@Test
	@DisplayName("청약 선납 성공")
	void paySubscription_prepayment_success() {
		Member parent3 = findParent3();
		Member kid2 = findKid2();

		Account freeAccount = findFreeAccount(parent3.getId());
		Account subscriptionAccount = findSubscriptionAccount(kid2.getId());

		BigDecimal freeBefore = freeAccount.getBalance();
		BigDecimal subscriptionBefore = subscriptionAccount.getBalance();

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			3,
			"123456",
			null
		);

		SubscriptionResponseDto result = subscriptionService.paySubscription(
			parent3.getId(),
			subscriptionAccount.getId(),
			request
		);

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("300000");
		assertThat(result.getPrepaymentCount()).isEqualTo(3);
		assertThat(result.getRewardAmount()).isEqualByComparingTo("0");
		assertThat(result.getRewardAccountNumber()).isNull();

		Account updatedFree = accountRepository.findById(freeAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("입출금 계좌를 다시 찾을 수 없습니다."));
		Account updatedSubscription = accountRepository.findById(subscriptionAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("청약 계좌를 다시 찾을 수 없습니다."));

		assertThat(updatedFree.getBalance()).isEqualByComparingTo(freeBefore.subtract(new BigDecimal("300000")));
		assertThat(updatedSubscription.getBalance()).isEqualByComparingTo(subscriptionBefore.add(new BigDecimal("300000")));

		Integer maxRoundNo = prepaymentDetailRepository.findMaxRoundNoByAccountId(subscriptionAccount.getId())
			.orElse(0);
		assertThat(maxRoundNo).isEqualTo(3);
	}

	@Test
	@DisplayName("청약 첫 납입 성공 - 25만 원 초과 시 예를 누르면 초과분은 리워드 계좌로 입금")
	void paySubscription_firstPayment_overMaxAmount_withReward_success() {
		Member parent1 = findParent1();
		Member kid1 = findKid1();

		Account freeAccount = findFreeAccount(parent1.getId());
		Account rewardAccount = findRewardAccount(parent1.getId());
		Account subscriptionAccount = findSubscriptionAccount(kid1.getId());

		BigDecimal freeBefore = freeAccount.getBalance();
		BigDecimal rewardBefore = rewardAccount.getBalance();
		BigDecimal subscriptionBefore = subscriptionAccount.getBalance();

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			null,
			"123456",
			true
		);

		SubscriptionResponseDto result = subscriptionService.paySubscription(
			parent1.getId(),
			subscriptionAccount.getId(),
			request
		);

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber()).isEqualTo(subscriptionAccount.getAccountNumber());
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("250000");
		assertThat(result.getRewardAmount()).isEqualByComparingTo("50000");
		assertThat(result.getRewardAccountNumber()).isEqualTo(rewardAccount.getAccountNumber());
		assertThat(result.getPrepaymentCount()).isNull();

		Account updatedFree = accountRepository.findById(freeAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("입출금 계좌를 다시 찾을 수 없습니다."));
		Account updatedReward = accountRepository.findById(rewardAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("리워드 계좌를 다시 찾을 수 없습니다."));
		Account updatedSubscription = accountRepository.findById(subscriptionAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("청약 계좌를 다시 찾을 수 없습니다."));

		assertThat(updatedFree.getBalance()).isEqualByComparingTo(freeBefore.subtract(new BigDecimal("300000")));
		assertThat(updatedReward.getBalance()).isEqualByComparingTo(rewardBefore.add(new BigDecimal("50000")));
		assertThat(updatedSubscription.getBalance()).isEqualByComparingTo(subscriptionBefore.add(new BigDecimal("250000")));

		Integer maxRoundNo = prepaymentDetailRepository.findMaxRoundNoByAccountId(subscriptionAccount.getId())
			.orElse(0);
		assertThat(maxRoundNo).isEqualTo(1);
	}

	@Test
	@DisplayName("청약 첫 납입 성공 - 25만 원 초과 시 아니요를 누르면 전액 청약 계좌로 납입")
	void paySubscription_firstPayment_overMaxAmount_withoutReward_success() {
		Member parent1 = findParent1();
		Member kid1 = findKid1();

		Account freeAccount = findFreeAccount(parent1.getId());
		Account rewardAccount = findRewardAccount(parent1.getId());
		Account subscriptionAccount = findSubscriptionAccount(kid1.getId());

		BigDecimal freeBefore = freeAccount.getBalance();
		BigDecimal rewardBefore = rewardAccount.getBalance();
		BigDecimal subscriptionBefore = subscriptionAccount.getBalance();

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			null,
			"123456",
			false
		);

		SubscriptionResponseDto result = subscriptionService.paySubscription(
			parent1.getId(),
			subscriptionAccount.getId(),
			request
		);

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber()).isEqualTo(subscriptionAccount.getAccountNumber());
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("300000");
		assertThat(result.getRewardAmount()).isEqualByComparingTo("0");
		assertThat(result.getRewardAccountNumber()).isNull();
		assertThat(result.getPrepaymentCount()).isNull();

		Account updatedFree = accountRepository.findById(freeAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("입출금 계좌를 다시 찾을 수 없습니다."));
		Account updatedReward = accountRepository.findById(rewardAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("리워드 계좌를 다시 찾을 수 없습니다."));
		Account updatedSubscription = accountRepository.findById(subscriptionAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("청약 계좌를 다시 찾을 수 없습니다."));

		assertThat(updatedFree.getBalance()).isEqualByComparingTo(freeBefore.subtract(new BigDecimal("300000")));
		assertThat(updatedReward.getBalance()).isEqualByComparingTo(rewardBefore);
		assertThat(updatedSubscription.getBalance()).isEqualByComparingTo(subscriptionBefore.add(new BigDecimal("300000")));

		Integer maxRoundNo = prepaymentDetailRepository.findMaxRoundNoByAccountId(subscriptionAccount.getId())
			.orElse(0);
		assertThat(maxRoundNo).isEqualTo(1);
	}

	@Test
	@DisplayName("청약 첫 납입 실패 - 25만 원 초과 시 리워드 계좌 입금 여부를 선택하지 않음")
	void paySubscription_firstPayment_overMaxAmount_fail_whenChoiceIsNull() {
		Member parent1 = findParent1();
		Member kid1 = findKid1();
		Account subscriptionAccount = findSubscriptionAccount(kid1.getId());

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			null,
			"123456",
			null
		);

		assertThatThrownBy(() ->
			subscriptionService.paySubscription(parent1.getId(), subscriptionAccount.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("25만 원 초과 시 리워드 계좌 입금 여부를 선택해주세요.");
	}

	@Test
	@DisplayName("청약 납입 실패 - 비밀번호 불일치")
	void paySubscription_fail_wrongPassword() {
		Member parent1 = findParent1();
		Member kid1 = findKid1();
		Account subscriptionAccount = findSubscriptionAccount(kid1.getId());

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("100000"),
			null,
			"000000",
			null
		);

		assertThatThrownBy(() ->
			subscriptionService.paySubscription(parent1.getId(), subscriptionAccount.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("비밀번호가 일치하지 않습니다.");
	}

	@Test
	@DisplayName("청약 납입 실패 - 잔액 부족")
	void paySubscription_fail_insufficientBalance() {
		Member parent1 = findParent1();
		Member kid1 = findKid1();
		Account subscriptionAccount = findSubscriptionAccount(kid1.getId());

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("900000"),
			null,
			"123456",
			true
		);

		assertThatThrownBy(() ->
			subscriptionService.paySubscription(parent1.getId(), subscriptionAccount.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("잔액이 부족합니다.");
	}

	@Test
	@DisplayName("최근 청약 납입 결과 조회 성공")
	void getSubscriptionPaymentResult_success() {
		Member parent3 = findParent3();
		Member kid2 = findKid2();
		Account subscriptionAccount = findSubscriptionAccount(kid2.getId());

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			1,
			"123456",
			null
		);

		subscriptionService.paySubscription(parent3.getId(), subscriptionAccount.getId(), request);

		SubscriptionResponseDto result =
			subscriptionService.getSubscriptionPaymentResult(parent3.getId(), subscriptionAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(subscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber())
			.isEqualTo(accountCryptoService.decrypt(subscriptionAccount.getAccountNumber()));
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("300000");
		assertThat(result.getPaidAt()).isNotNull();
	}
}
