package com.hanaro.hanaconnect;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.House;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Mission;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.HouseRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;
import com.hanaro.hanaconnect.service.AccountHashService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InitLoader implements ApplicationRunner {

	private final AccountRepository accountRepository;
	private final MemberRepository memberRepository;
	private final RelationRepository relationRepository;
	private final PhoneNameRepository phoneNameRepository;
	private final AccountCryptoService accountCryptoService;
	private final PasswordEncoder passwordEncoder;
	private final MissionRepository missionRepository;
	private final HouseRepository houseRepository;
	private final LinkedAccountRepository linkedAccountRepository;
	private final TransactionRepository transactionRepository;
	private final AccountHashService accountHashService;

	@Override
	@Transactional
	public void run(@Nullable ApplicationArguments args) {

		String encodedPassword = passwordEncoder.encode("123456");

		Member kid1 = createMember(
			"홍길동",
			encodedPassword,
			LocalDate.of(2010, 1, 2),
			"11122223333",
			MemberRole.KID
		);

		Member kid2 = createMember(
			"김청약",
			encodedPassword,
			LocalDate.of(2012, 3, 1),
			"77770000111",
			MemberRole.KID
		);

		Member parent1 = createMember(
			"김엄마",
			encodedPassword,
			LocalDate.of(1980, 5, 19),
			"22233334444",
			MemberRole.PARENT
		);

		Member parent2 = createMember(
			"이할머니",
			encodedPassword,
			LocalDate.of(1952, 8, 31),
			"33344445555",
			MemberRole.PARENT
		);

		Member parent3 = createMember(
			"청약할머니",
			encodedPassword,
			LocalDate.of(1958, 7, 15),
			"88880000111",
			MemberRole.PARENT
		);

		kid1 = memberRepository.save(kid1);
		kid2 = memberRepository.save(kid2);
		parent1 = memberRepository.save(parent1);
		parent2 = memberRepository.save(parent2);
		parent3 = memberRepository.save(parent3);

		// kid1
		Account kid1WalletAccount = accountRepository.save(createAccount(
			"홍길동 지갑",
			"11122220000",
			"1234",
			AccountType.WALLET,
			new BigDecimal("30000"),
			kid1,
			null
		));

		Account kid1FreeAccount = accountRepository.save(createAccount(
			"아이 입출금 통장",
			"11122223333",
			"1234",
			AccountType.FREE,
			new BigDecimal("50000"),
			kid1,
			null
		));

		Account kid1SavingsAccount = accountRepository.save(createAccount(
			"아이 적금 통장",
			"66677778888",
			"1234",
			AccountType.SAVINGS,
			new BigDecimal("80000"),
			kid1,
			new BigDecimal("300000")
		));

		Account kid1SubscriptionAccount = accountRepository.save(createAccount(
			"아이 청약 통장",
			"77788881111",
			"1234",
			AccountType.SUBSCRIPTION,
			new BigDecimal("120000"),
			kid1,
			null
		));

		Account kid1GiftSavingsAccount = accountRepository.save(createAccount(
			"채현이 적금 (용돈)",
			"11133334444",
			"1234",
			AccountType.SAVINGS,
			new BigDecimal("250000"),
			kid1,
			new BigDecimal("300000")
		));

		// kid2
		Account kid2WalletAccount = accountRepository.save(createAccount(
			"김청약 지갑",
			"99900000000",
			"4321",
			AccountType.WALLET,
			new BigDecimal("20000"),
			kid2,
			null
		));

		Account kid2HousingAccount = accountRepository.save(createAccount(
			"김청약 주택청약",
			"99900001111",
			"4321",
			AccountType.SUBSCRIPTION,
			new BigDecimal("5600000"),
			kid2,
			null
		));

		// parent1
		Account parent1WalletAccount = accountRepository.save(createAccount(
			"김엄마 지갑",
			"22233330000",
			"5678",
			AccountType.WALLET,
			new BigDecimal("830000"),
			parent1,
			null
		));

		Account parent1FreeAccount = accountRepository.save(createAccount(
			"부모 입출금 통장",
			"22233335555",
			"5678",
			AccountType.FREE,
			new BigDecimal("800000"),
			parent1,
			null
		));

		Account parent1RewardAccount = accountRepository.save(Account.builder()
			.name("부모 리워드 통장")
			.accountNumber(accountCryptoService.encrypt("22233336666"))
			.accountNumberHash(accountHashService.hash("22233336666"))
			.password(passwordEncoder.encode("5678"))
			.accountType(AccountType.PENSION)
			.balance(BigDecimal.ZERO)
			.member(parent1)
			.isReward(true)
			.build());

		Account parent1DepositAccount = accountRepository.save(createAccount(
			"부모 저축 예금",
			"22233334444",
			"5678",
			AccountType.DEPOSIT,
			new BigDecimal("100000"),
			parent1,
			null
		));

		Account parent1SubscriptionAccount = accountRepository.save(createAccount(
			"부모 청약 통장",
			"44455556666",
			"5678",
			AccountType.SUBSCRIPTION,
			new BigDecimal("150000"),
			parent1,
			null
		));

		Account parent1PensionAccount = accountRepository.save(createAccount(
			"부모 연금 통장",
			"55566667777",
			"5678",
			AccountType.PENSION,
			new BigDecimal("300000"),
			parent1,
			null
		));

		// parent2
		Account parent2WalletAccount = accountRepository.save(createAccount(
			"이할머니 지갑",
			"33344450000",
			"1234",
			AccountType.WALLET,
			new BigDecimal("750000"),
			parent2,
			null
		));

		Account parent2FreeAccount = accountRepository.save(createAccount(
			"할머니 입출금 통장",
			"33344455566",
			"1234",
			AccountType.FREE,
			new BigDecimal("900000"),
			parent2,
			null
		));

		// parent3
		Account parent3WalletAccount = accountRepository.save(createAccount(
			"청약할머니 지갑",
			"77788880001",
			"1234",
			AccountType.WALLET,
			new BigDecimal("990000"),
			parent3,
			null
		));

		Account parent3FreeAccount = accountRepository.save(createAccount(
			"청약할머니 입출금 통장",
			"77788889999",
			"1234",
			AccountType.FREE,
			new BigDecimal("10000000"),
			parent3,
			null
		));

		Account parent3RewardAccount = accountRepository.save(Account.builder()
			.name("청약할머니 리워드 통장")
			.accountNumber(accountCryptoService.encrypt("77788880000"))
			.accountNumberHash(accountHashService.hash("77788880000"))
			.password(passwordEncoder.encode("1234"))
			.accountType(AccountType.PENSION)
			.balance(BigDecimal.ZERO)
			.member(parent3)
			.isReward(true)
			.build());

		// 만기 적금 샘플
		Account kid1MaturedSavings1 = accountRepository.save(Account.builder()
			.name("청춘 적금(만기)")
			.accountNumber(accountCryptoService.encrypt("12345678901"))
			.accountNumberHash(accountHashService.hash("12345678901"))
			.password(passwordEncoder.encode("1234"))
			.accountType(AccountType.SAVINGS)
			.balance(new BigDecimal("2000000"))
			.member(kid1)
			.isEnd(true)
			.build());

		Account kid1MaturedSavings2 = accountRepository.save(Account.builder()
			.name("하나 새희망 적금")
			.accountNumber(accountCryptoService.encrypt("12121212121"))
			.accountNumberHash(accountHashService.hash("12121212121"))
			.password(passwordEncoder.encode("1234"))
			.accountType(AccountType.SAVINGS)
			.balance(new BigDecimal("5000000"))
			.member(kid1)
			.isEnd(true)
			.build());

		// linked accounts
		link(kid1, kid1WalletAccount);
		link(kid1, kid1FreeAccount);
		link(kid1, kid1SavingsAccount);
		link(kid1, kid1GiftSavingsAccount);
		link(kid1, kid1SubscriptionAccount);
		link(kid1, kid1MaturedSavings1);
		link(kid1, kid1MaturedSavings2);

		link(parent1, parent1WalletAccount);
		link(parent1, parent1FreeAccount);
		link(parent1, parent1DepositAccount);
		link(parent1, parent1RewardAccount);
		link(parent1, parent1SubscriptionAccount);
		link(parent1, parent1PensionAccount);
		link(parent1, kid1FreeAccount);
		link(parent1, kid1GiftSavingsAccount);
		link(parent1, kid1SubscriptionAccount);

		link(parent2, parent2WalletAccount);
		link(parent2, parent2FreeAccount);
		link(parent2, kid1SavingsAccount);

		link(kid2, kid2WalletAccount);
		link(kid2, kid2HousingAccount);

		link(parent3, parent3WalletAccount);
		link(parent3, parent3FreeAccount);
		link(parent3, parent3RewardAccount);
		link(parent3, kid2HousingAccount);

		// relations
		relationRepository.save(createRelation(kid1, parent1));
		relationRepository.save(createRelation(kid1, parent2));
		relationRepository.save(createRelation(parent1, kid1));
		relationRepository.save(createRelation(parent1, parent2));
		relationRepository.save(createRelation(parent2, kid1));
		relationRepository.save(createRelation(parent2, parent1));
		relationRepository.save(createRelation(kid2, parent3));
		relationRepository.save(createRelation(parent3, kid2));

		// phone names
		phoneNameRepository.save(createPhoneName(kid1, parent1, "우리 엄마"));
		phoneNameRepository.save(createPhoneName(kid1, parent2, "외할머니"));
		phoneNameRepository.save(createPhoneName(parent1, parent2, "친정 엄마"));
		phoneNameRepository.save(createPhoneName(parent1, kid1, "우리 아들"));
		phoneNameRepository.save(createPhoneName(parent2, kid1, "손주"));
		phoneNameRepository.save(createPhoneName(parent2, parent1, "딸"));
		phoneNameRepository.save(createPhoneName(kid2, parent3, "할머니"));
		phoneNameRepository.save(createPhoneName(parent3, kid2, "김청약"));

		houseRepository.save(
			House.builder()
				.member(kid2)
				.account(kid2HousingAccount)
				.level(3)
				.totalCount(28)
				.monthlyPayment(new BigDecimal("200000"))
				.startDate(LocalDate.of(2024, 1, 25))
				.build()
		);

		// 앱 내 송금/납입 성격 거래는 WALLET에서 나가도록 맞춤
		createCheongyakTransactions(parent3WalletAccount, kid2HousingAccount);

		createSampleMissions(kid1, parent1);
	}

	private Member createMember(
		String name,
		String encodedPassword,
		LocalDate birthday,
		String rawVirtualAccount,
		MemberRole memberRole
	) {
		String encryptedAccount = accountCryptoService.encrypt(rawVirtualAccount);

		return Member.builder()
			.name(name)
			.password(encodedPassword)
			.birthday(birthday)
			.virtualAccount(encryptedAccount)
			.walletMoney(BigDecimal.ZERO) // 더 이상 실사용 X
			.memberRole(memberRole)
			.role(Role.USER)
			.build();
	}

	private Account createAccount(
		String name,
		String rawAccountNumber,
		String rawPassword,
		AccountType accountType,
		BigDecimal balance,
		Member member,
		BigDecimal totalLimit
	) {
		String normalized = AccountNumberFormatter.normalize(rawAccountNumber);

		String encryptedAccount = accountCryptoService.encrypt(normalized);
		String accountNumberHash = accountHashService.hash(normalized);

		return Account.builder()
			.name(name)
			.accountNumber(encryptedAccount)
			.accountNumberHash(accountNumberHash)
			.password(passwordEncoder.encode(rawPassword))
			.accountType(accountType)
			.balance(balance)
			.member(member)
			.totalLimit(totalLimit)
			.isReward(false)
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

	private Relation createRelation(Member member, Member connectMember) {
		return Relation.builder()
			.member(member)
			.connectMember(connectMember)
			.connectMemberRole(connectMember.getMemberRole())
			.build();
	}

	private PhoneName createPhoneName(Member who, Member whom, String whomName) {
		return PhoneName.builder()
			.who(who)
			.whom(whom)
			.whomName(whomName)
			.build();
	}

	private void createSampleMissions(Member kid, Member parent) {
		missionRepository.saveAll(List.of(
			Mission.builder().kid(kid).parent(parent).name("부모님께 인사하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("심부름 다녀오기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("용돈 기록 작성하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("방 정리하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("오늘 소비 내역 확인하기").isCompleted(true).build()
		));
	}

	private void createCheongyakTransactions(Account senderAccount, Account receiverAccount) {
		LocalDate startDate = LocalDate.of(2024, 1, 12);

		for (int i = 0; i < 28; i++) {
			LocalDate paymentDate = startDate.plusMonths(i);

			Transaction transaction = Transaction.builder()
				.transactionMoney(new BigDecimal("200000"))
				.transactionBalance(new BigDecimal(200000L * (i + 1)))
				.transactionType(TransactionType.SUBSCRIPTION)
				.senderAccount(senderAccount)
				.receiverAccount(receiverAccount)
				.build();

			transaction.setCreatedAtForInit(paymentDate.atTime(12, 0));
			transactionRepository.save(transaction);
		}
	}
}
