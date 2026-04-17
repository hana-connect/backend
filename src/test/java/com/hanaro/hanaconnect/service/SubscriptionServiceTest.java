package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionRequestDto;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.House;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.HouseRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.PrepaymentDetailRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

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
	private RelationRepository relationRepository;

	@Autowired
	private LinkedAccountRepository linkedAccountRepository;

	@Autowired
	private HouseRepository houseRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private PrepaymentDetailRepository prepaymentDetailRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccountCryptoService accountCryptoService;

	@Autowired
	private AccountHashService accountHashService;

	private static long virtualAccountSeq = 90000000000L;

	private Member kid1;
	private Member kid2;
	private Member parent1;
	private Member parent3;

	private Account kid1SubscriptionAccount;
	private Account kid1FreeAccount;

	private Account kid2SubscriptionAccount;

	private Account parent1WalletAccount;
	private Account parent1RewardAccount;

	private Account parent3WalletAccount;
	private Account parent3RewardAccount;

	@BeforeEach
	void setUp() {
		// 회원 생성
		kid1 = createMember(
			"홍길동",
			LocalDate.of(2010, 1, 2),
			MemberRole.KID
		);

		kid2 = createMember(
			"김청약",
			LocalDate.of(2012, 3, 1),
			MemberRole.KID
		);

		parent1 = createMember(
			"김엄마",
			LocalDate.of(1980, 5, 19),
			MemberRole.PARENT
		);

		parent3 = createMember(
			"청약할머니",
			LocalDate.of(1958, 7, 15),
			MemberRole.PARENT
		);

		// 관계 생성
		relationRepository.save(createRelation(kid1, parent1));
		relationRepository.save(createRelation(parent1, kid1));

		relationRepository.save(createRelation(kid2, parent3));
		relationRepository.save(createRelation(parent3, kid2));

		// kid1 계좌
		createAccount(
			"홍길동 지갑",
			"1234",
			AccountType.WALLET,
			new BigDecimal("30000"),
			kid1,
			null,
			false
		);

		kid1FreeAccount = createAccount(
			"아이 입출금 통장",
			"1234",
			AccountType.FREE,
			new BigDecimal("50000"),
			kid1,
			null,
			false
		);

		kid1SubscriptionAccount = createAccount(
			"아이 청약 통장",
			"1234",
			AccountType.SUBSCRIPTION,
			new BigDecimal("120000"),
			kid1,
			null,
			false
		);

		// kid2 계좌
		createAccount(
			"김청약 지갑",
			"4321",
			AccountType.WALLET,
			new BigDecimal("20000"),
			kid2,
			null,
			false
		);

		kid2SubscriptionAccount = createAccount(
			"김청약 주택청약",
			"4321",
			AccountType.SUBSCRIPTION,
			new BigDecimal("5600000"),
			kid2,
			null,
			false
		);

		// parent1 계좌
		parent1WalletAccount = createAccount(
			"김엄마 지갑",
			"123456",
			AccountType.WALLET,
			new BigDecimal("800000"),
			parent1,
			null,
			false
		);

		createAccount(
			"부모 입출금 통장",
			"123456",
			AccountType.FREE,
			new BigDecimal("800000"),
			parent1,
			null,
			false
		);

		parent1RewardAccount = createAccount(
			"부모 리워드 통장",
			"123456",
			AccountType.PENSION,
			BigDecimal.ZERO,
			parent1,
			null,
			true
		);

		// parent3 계좌
		parent3WalletAccount = createAccount(
			"청약할머니 지갑",
			"123456",
			AccountType.WALLET,
			new BigDecimal("10000000"),
			parent3,
			null,
			false
		);

		createAccount(
			"청약할머니 입출금 통장",
			"123456",
			AccountType.FREE,
			new BigDecimal("10000000"),
			parent3,
			null,
			false
		);

		parent3RewardAccount = createAccount(
			"청약할머니 리워드 통장",
			"123456",
			AccountType.PENSION,
			BigDecimal.ZERO,
			parent3,
			null,
			true
		);

		// linked account
		link(kid1, kid1SubscriptionAccount);
		link(kid1, kid1FreeAccount);

		link(parent1, parent1WalletAccount);
		link(parent1, parent1RewardAccount);
		link(parent1, kid1SubscriptionAccount);
		link(parent1, kid1FreeAccount);

		link(kid2, kid2SubscriptionAccount);

		link(parent3, parent3WalletAccount);
		link(parent3, parent3RewardAccount);
		link(parent3, kid2SubscriptionAccount);

		// kid2 청약용 house
		houseRepository.save(
			House.builder()
				.member(kid2)
				.account(kid2SubscriptionAccount)
				.level(3)
				.totalCount(28)
				.monthlyPayment(new BigDecimal("200000"))
				.startDate(LocalDate.of(2024, 1, 25))
				.build()
		);

		// "이번 달 이미 납입함" 시나리오용 거래 생성
		createSubscriptionTransaction(
			parent3WalletAccount,
			kid2SubscriptionAccount,
			new BigDecimal("200000"),
			LocalDate.now().withDayOfMonth(5),
			new BigDecimal("5800000")
		);
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

	private Account createAccount(
		String name,
		String rawPassword,
		AccountType accountType,
		BigDecimal balance,
		Member member,
		BigDecimal totalLimit,
		boolean isReward
	) {
		String rawAccountNumber = generateVirtualAccount();

		Account account = Account.builder()
			.name(name)
			.accountNumber(accountCryptoService.encrypt(rawAccountNumber))
			.accountNumberHash(accountHashService.hash(rawAccountNumber))
			.password(passwordEncoder.encode(rawPassword))
			.accountType(accountType)
			.balance(balance)
			.member(member)
			.totalLimit(totalLimit)
			.isReward(isReward)
			.build();

		return accountRepository.save(account);
	}

	private Relation createRelation(Member member, Member connectMember) {
		return Relation.builder()
			.member(member)
			.connectMember(connectMember)
			.connectMemberRole(connectMember.getMemberRole())
			.build();
	}

	private void link(Member member, Account account) {
		linkedAccountRepository.save(
			LinkedAccount.builder()
				.member(member)
				.account(account)
				.build()
		);
	}

	private void createSubscriptionTransaction(
		Account senderAccount,
		Account receiverAccount,
		BigDecimal money,
		LocalDate date,
		BigDecimal balanceAfter
	) {
		Transaction transaction = Transaction.builder()
			.transactionMoney(money)
			.transactionBalance(balanceAfter)
			.transactionType(TransactionType.SUBSCRIPTION)
			.senderAccount(senderAccount)
			.receiverAccount(receiverAccount)
			.build();

		transaction.setCreatedAtForInit(date.atTime(12, 0));
		transactionRepository.save(transaction);
	}

	private String generateVirtualAccount() {
		return String.valueOf(virtualAccountSeq++);
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
		SubscriptionInfoResponseDto result =
			subscriptionService.getSubscriptionPaymentInfo(kid1.getId(), kid1SubscriptionAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(kid1SubscriptionAccount.getId());
		assertThat(result.getAccountNumber()).isEqualTo(kid1SubscriptionAccount.getAccountNumber());
		assertThat(result.isHasPaidThisMonth()).isFalse();
		assertThat(result.getAlreadyPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("청약 납입 정보 조회 성공 - 이번 달 납입 이력 있음")
	void getSubscriptionPaymentInfoSuccessHasPaidThisMonthTest() {
		SubscriptionInfoResponseDto result =
			subscriptionService.getSubscriptionPaymentInfo(parent3.getId(), kid2SubscriptionAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(kid2SubscriptionAccount.getId());
		assertThat(result.getAccountNumber()).isEqualTo(kid2SubscriptionAccount.getAccountNumber());
		assertThat(result.isHasPaidThisMonth()).isTrue();
		assertThat(result.getAlreadyPaidAmount()).isGreaterThan(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("청약 진입 정보 조회 성공 - 표시 정보 포함")
	void getSubscriptionPaymentInfoSuccessWithDisplayFieldsTest() {
		SubscriptionInfoResponseDto result =
			subscriptionService.getSubscriptionPaymentInfo(parent3.getId(), kid2SubscriptionAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(kid2SubscriptionAccount.getId());
		assertThat(result.getAccountNumber()).isEqualTo(kid2SubscriptionAccount.getAccountNumber());
		assertThat(result.getAlreadyPaidAmount()).isNotNull();
		assertThat(result.getDisplayName()).contains("김청약");
		assertThat(result.getAccountNickname()).isEqualTo(kid2SubscriptionAccount.getName());
		assertThat(result.getRewardAccountName()).isEqualTo(parent3RewardAccount.getName());
	}

	@Test
	@DisplayName("청약 납입 정보 조회 실패 - 청약 계좌가 아님")
	void getSubscriptionPaymentInfoFailNotSubscriptionAccountTest() {
		assertThatThrownBy(() ->
			subscriptionService.getSubscriptionPaymentInfo(kid1.getId(), kid1FreeAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("청약 계좌가 아닙니다.");
	}

	@Test
	@DisplayName("청약 납입 정보 조회 실패 - 존재하지 않는 청약 계좌")
	void getSubscriptionPaymentInfoFailAccountNotFoundTest() {
		assertThatThrownBy(() ->
			subscriptionService.getSubscriptionPaymentInfo(kid1.getId(), 999999L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("청약 계좌를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("청약 첫 납입 성공")
	void paySubscription_firstPayment_success() {
		BigDecimal walletBefore = parent1WalletAccount.getBalance();
		BigDecimal subscriptionBefore = kid1SubscriptionAccount.getBalance();

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("100000"),
			null,
			"123456",
			null
		);

		SubscriptionResponseDto result = subscriptionService.paySubscription(
			parent1.getId(),
			kid1SubscriptionAccount.getId(),
			request
		);

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(kid1SubscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber())
			.isEqualTo(accountCryptoService.decrypt(kid1SubscriptionAccount.getAccountNumber()));
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("100000");
		assertThat(result.getRewardAmount()).isEqualByComparingTo("0");
		assertThat(result.getRewardAccountNumber()).isNull();
		assertThat(result.getPrepaymentCount()).isNull();

		Account updatedWallet = accountRepository.findById(parent1WalletAccount.getId()).orElseThrow();
		Account updatedSubscription = accountRepository.findById(kid1SubscriptionAccount.getId()).orElseThrow();

		assertThat(updatedWallet.getBalance())
			.isEqualByComparingTo(walletBefore.subtract(new BigDecimal("100000")));
		assertThat(updatedSubscription.getBalance())
			.isEqualByComparingTo(subscriptionBefore.add(new BigDecimal("100000")));

		Integer maxRoundNo = prepaymentDetailRepository.findMaxRoundNoByAccountId(kid1SubscriptionAccount.getId())
			.orElse(0);
		assertThat(maxRoundNo).isEqualTo(1);
	}

	@Test
	@DisplayName("청약 선납 성공")
	void paySubscription_prepayment_success() {
		BigDecimal walletBefore = parent3WalletAccount.getBalance();
		BigDecimal subscriptionBefore = kid2SubscriptionAccount.getBalance();

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			3,
			"123456",
			null
		);

		SubscriptionResponseDto result = subscriptionService.paySubscription(
			parent3.getId(),
			kid2SubscriptionAccount.getId(),
			request
		);

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(kid2SubscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber())
			.isEqualTo(accountCryptoService.decrypt(kid2SubscriptionAccount.getAccountNumber()));
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("300000");
		assertThat(result.getPrepaymentCount()).isEqualTo(3);
		assertThat(result.getRewardAmount()).isEqualByComparingTo("0");
		assertThat(result.getRewardAccountNumber()).isNull();

		Account updatedWallet = accountRepository.findById(parent3WalletAccount.getId()).orElseThrow();
		Account updatedSubscription = accountRepository.findById(kid2SubscriptionAccount.getId()).orElseThrow();

		assertThat(updatedWallet.getBalance())
			.isEqualByComparingTo(walletBefore.subtract(new BigDecimal("300000")));
		assertThat(updatedSubscription.getBalance())
			.isEqualByComparingTo(subscriptionBefore.add(new BigDecimal("300000")));

		Integer maxRoundNo = prepaymentDetailRepository.findMaxRoundNoByAccountId(kid2SubscriptionAccount.getId())
			.orElse(0);
		assertThat(maxRoundNo).isEqualTo(3);
	}

	@Test
	@DisplayName("청약 첫 납입 성공 - 25만 원 초과 시 예를 누르면 초과분은 리워드 계좌로 입금")
	void paySubscription_firstPayment_overMaxAmount_withReward_success() {
		BigDecimal walletBefore = parent1WalletAccount.getBalance();
		BigDecimal rewardBefore = parent1RewardAccount.getBalance();
		BigDecimal subscriptionBefore = kid1SubscriptionAccount.getBalance();

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			null,
			"123456",
			true
		);

		SubscriptionResponseDto result = subscriptionService.paySubscription(
			parent1.getId(),
			kid1SubscriptionAccount.getId(),
			request
		);

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(kid1SubscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber())
			.isEqualTo(accountCryptoService.decrypt(kid1SubscriptionAccount.getAccountNumber()));
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("250000");
		assertThat(result.getRewardAmount()).isEqualByComparingTo("50000");
		assertThat(result.getRewardAccountNumber())
			.isEqualTo(accountCryptoService.decrypt(parent1RewardAccount.getAccountNumber()));
		assertThat(result.getPrepaymentCount()).isNull();

		Account updatedWallet = accountRepository.findById(parent1WalletAccount.getId()).orElseThrow();
		Account updatedReward = accountRepository.findById(parent1RewardAccount.getId()).orElseThrow();
		Account updatedSubscription = accountRepository.findById(kid1SubscriptionAccount.getId()).orElseThrow();

		assertThat(updatedWallet.getBalance())
			.isEqualByComparingTo(walletBefore.subtract(new BigDecimal("300000")));
		assertThat(updatedReward.getBalance())
			.isEqualByComparingTo(rewardBefore.add(new BigDecimal("50000")));
		assertThat(updatedSubscription.getBalance())
			.isEqualByComparingTo(subscriptionBefore.add(new BigDecimal("250000")));

		Integer maxRoundNo = prepaymentDetailRepository.findMaxRoundNoByAccountId(kid1SubscriptionAccount.getId())
			.orElse(0);
		assertThat(maxRoundNo).isEqualTo(1);
	}

	@Test
	@DisplayName("청약 첫 납입 성공 - 25만 원 초과 시 아니요를 누르면 전액 청약 계좌로 납입")
	void paySubscription_firstPayment_overMaxAmount_withoutReward_success() {
		BigDecimal walletBefore = parent1WalletAccount.getBalance();
		BigDecimal rewardBefore = parent1RewardAccount.getBalance();
		BigDecimal subscriptionBefore = kid1SubscriptionAccount.getBalance();

		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			null,
			"123456",
			false
		);

		SubscriptionResponseDto result = subscriptionService.paySubscription(
			parent1.getId(),
			kid1SubscriptionAccount.getId(),
			request
		);

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(kid1SubscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber())
			.isEqualTo(accountCryptoService.decrypt(kid1SubscriptionAccount.getAccountNumber()));
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("300000");
		assertThat(result.getRewardAmount()).isEqualByComparingTo("0");
		assertThat(result.getRewardAccountNumber()).isNull();
		assertThat(result.getPrepaymentCount()).isNull();

		Account updatedWallet = accountRepository.findById(parent1WalletAccount.getId()).orElseThrow();
		Account updatedReward = accountRepository.findById(parent1RewardAccount.getId()).orElseThrow();
		Account updatedSubscription = accountRepository.findById(kid1SubscriptionAccount.getId()).orElseThrow();

		assertThat(updatedWallet.getBalance())
			.isEqualByComparingTo(walletBefore.subtract(new BigDecimal("300000")));
		assertThat(updatedReward.getBalance()).isEqualByComparingTo(rewardBefore);
		assertThat(updatedSubscription.getBalance())
			.isEqualByComparingTo(subscriptionBefore.add(new BigDecimal("300000")));

		Integer maxRoundNo = prepaymentDetailRepository.findMaxRoundNoByAccountId(kid1SubscriptionAccount.getId())
			.orElse(0);
		assertThat(maxRoundNo).isEqualTo(1);
	}

	@Test
	@DisplayName("청약 첫 납입 실패 - 25만 원 초과 시 리워드 계좌 입금 여부를 선택하지 않음")
	void paySubscription_firstPayment_overMaxAmount_fail_whenChoiceIsNull() {
		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			null,
			"123456",
			null
		);

		assertThatThrownBy(() ->
			subscriptionService.paySubscription(parent1.getId(), kid1SubscriptionAccount.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("25만 원 초과 시 리워드 계좌 입금 여부를 선택해주세요.");
	}

	@Test
	@DisplayName("청약 납입 실패 - 비밀번호 불일치")
	void paySubscription_fail_wrongPassword() {
		SubscriptionRequestDto request = createRequest(
			new BigDecimal("100000"),
			null,
			"000000",
			null
		);

		assertThatThrownBy(() ->
			subscriptionService.paySubscription(parent1.getId(), kid1SubscriptionAccount.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("비밀번호가 일치하지 않습니다.");
	}

	@Test
	@DisplayName("청약 납입 실패 - 잔액 부족")
	void paySubscription_fail_insufficientBalance() {
		SubscriptionRequestDto request = createRequest(
			new BigDecimal("20000000"),
			null,
			"123456",
			true
		);

		assertThatThrownBy(() ->
			subscriptionService.paySubscription(parent1.getId(), kid1SubscriptionAccount.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("잔액이 부족합니다.");
	}

	@Test
	@DisplayName("최근 청약 납입 결과 조회 성공")
	void getSubscriptionPaymentResult_success() {
		SubscriptionRequestDto request = createRequest(
			new BigDecimal("300000"),
			1,
			"123456",
			null
		);

		subscriptionService.paySubscription(parent3.getId(), kid2SubscriptionAccount.getId(), request);

		SubscriptionResponseDto result =
			subscriptionService.getSubscriptionPaymentResult(parent3.getId(), kid2SubscriptionAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getSubscriptionId()).isEqualTo(kid2SubscriptionAccount.getId());
		assertThat(result.getSubscriptionAccountNumber())
			.isEqualTo(accountCryptoService.decrypt(kid2SubscriptionAccount.getAccountNumber()));
		assertThat(result.getSubscriptionAmount()).isEqualByComparingTo("300000");
		assertThat(result.getPaidAt()).isNotNull();
	}
}
