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
import com.hanaro.hanaconnect.common.security.AccountCryptoService;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.House;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Mission;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.HouseRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

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
			"할청약",
			encodedPassword,
			LocalDate.of(1958, 7, 15),
			"88880000111",
			MemberRole.PARENT
		);

		// 객체를 DB에 저장
		kid1 = memberRepository.save(kid1);
		kid2 = memberRepository.save(kid2);
		parent1 = memberRepository.save(parent1);
		parent2 = memberRepository.save(parent2);
		parent3 = memberRepository.save(parent3);

		Account kidAccount = accountRepository.save(createAccount(
			"아이 입출금 통장",
			"11122223333",
			"1234",
			AccountType.FREE,
			new BigDecimal("50000"),
			kid1,
			null
		));

		accountRepository.save(createAccount(
			"아이 적금 통장",
			"66677778888",
			"1234",
			AccountType.SAVINGS,
			new BigDecimal("80000"),
			kid1,
			null
		));

		accountRepository.save(createAccount(
			"아이 청약 통장",
			"77788889999",
			"1234",
			AccountType.SUBSCRIPTION,
			new BigDecimal("120000"),
			kid1,
			null
		));


		// 부모1 입출금 계좌
		Account parentFreeAccount = accountRepository.save(createAccount(
			"부모 입출금 통장",
			"22233335555",
			"5678",
			AccountType.FREE,
			new BigDecimal("800000"),
			parent1,
			null
		));

		// 부모1 저축 예금 계좌
		Account parentDepositAccount = accountRepository.save(createAccount(
			"부모 저축 예금",
			"22233334444",
			"5678",
			AccountType.DEPOSIT,
			new BigDecimal("100000"),
			parent1,
			null
		));

		Account kidSavingsAccount = accountRepository.save(createAccount(
			"채현이 적금 (용돈)",
			"11133334444",
			"1234",
			AccountType.SAVINGS,
			new BigDecimal("250000"),
			kid1,
			new BigDecimal("300000")
		));

		accountRepository.save(createAccount(
			"할머니 지갑",
			"33344455566",
			"1234",
			AccountType.FREE,
			new BigDecimal("900000"),
			parent2,
			null
		));

		accountRepository.save(createAccount(
			"부모 청약 통장",
			"44455556666",
			"5678",
			AccountType.SUBSCRIPTION,
			new BigDecimal("150000"),
			parent1,
			null
		));

		Account parent3FreeAccount = accountRepository.save(createAccount(
			"할청약 입출금 통장",
			"777788889999",
			"1234",
			AccountType.FREE,
			new BigDecimal("10000000"),
			parent3,
			null
		));

		Account kid2HousingAccount = accountRepository.save(createAccount(
			"김청약 주택청약",
			"999900001111",
			"4321",
			AccountType.SUBSCRIPTION,
			new BigDecimal("5600000"),
			kid2,
			null
		));

		accountRepository.save(createAccount(
			"부모 연금 통장",
			"55566667777",
			"5678",
			AccountType.PENSION,
			new BigDecimal("300000"),
			parent1,
			null
		));

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.account(kidAccount)
				.member(kid1)
				.build()
		);

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.account(kidAccount)
				.member(parent1)
				.build()
		);

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.account(parentDepositAccount)
				.member(parent1)
				.build()
		);

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.account(parentFreeAccount)
				.member(parent1)
				.build()
		);

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.account(kidSavingsAccount)
				.member(kid1)
				.build()
		);

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.account(kidSavingsAccount)
				.member(parent1)
				.build()
		);

		System.out.println("kid1 = " + kid1);
		System.out.println("kid2 = " + kid2);
		System.out.println("parent1 = " + parent1);
		System.out.println("parent2 = " + parent2);
		System.out.println("parent3 = " + parent3);

		relationRepository.save(createRelation(kid1, parent1));
		relationRepository.save(createRelation(kid1, parent2));

		relationRepository.save(createRelation(parent1, kid1));
		relationRepository.save(createRelation(parent1, parent2));

		relationRepository.save(createRelation(parent2, kid1));
		relationRepository.save(createRelation(parent2, parent1));

		// 청약이들 입장에서 보이는 연결
		relationRepository.save(createRelation(kid2, parent3));
		relationRepository.save(createRelation(parent3, kid2));

		phoneNameRepository.save(createPhoneName(kid1, parent1, "우리 엄마"));
		phoneNameRepository.save(createPhoneName(kid1, parent2, "외할머니"));
		phoneNameRepository.save(createPhoneName(parent1, parent2, "친정 엄마"));
		phoneNameRepository.save(createPhoneName(parent1, kid1, "우리 아들"));
		phoneNameRepository.save(createPhoneName(parent2, kid1, "손주"));
		phoneNameRepository.save(createPhoneName(parent2, parent1, "딸"));
		phoneNameRepository.save(createPhoneName(kid2, parent3, "할머니"));
		phoneNameRepository.save(createPhoneName(parent3, kid2, "김청약"));

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.member(parent3)
				.account(parent3FreeAccount)
				.build()
			);

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.member(kid2)
				.account(kid2HousingAccount)
				.build()
		);

		linkedAccountRepository.save(
				LinkedAccount.builder()
				.member(parent3)
				.account(kid2HousingAccount)
				.build()
		);

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

		createCheongyakTransactions(parent3FreeAccount, kid2HousingAccount);

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
			.walletMoney(BigDecimal.ZERO)
			.memberRole(memberRole)
			.role(Role.USER)
			.build();
	}

	private Account createAccount(
		String name,
		String accountNumber,
		String rawPassword,
		AccountType accountType,
		BigDecimal balance,
		Member member,
		BigDecimal totalLimit
	) {
		return Account.builder()
			.name(name)
			.accountNumber(accountNumber)
			.password(passwordEncoder.encode(rawPassword))
			.accountType(accountType)
			.balance(balance)
			.member(member)
			.totalLimit(totalLimit)
			.build();
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
			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("부모님께 인사하기")
				.isCompleted(true)
				.build(),

			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("심부름 다녀오기")
				.isCompleted(true)
				.build(),

			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("용돈 기록 작성하기")
				.isCompleted(true)
				.build(),

			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("방 정리하기")
				.isCompleted(true)
				.build(),

			Mission.builder()
		        .kid(kid)
		        .parent(parent)
		        .name("오늘 소비 내역 확인하기")
		        .isCompleted(true)
		        .build()
    ));
  }
	private void createCheongyakTransactions(Account senderAccount, Account receiverAccount) {
		LocalDate startDate = LocalDate.of(2024, 1, 12);

		for (int i = 0; i < 28; i++) {
			LocalDate paymentDate = startDate.plusMonths(i);

			com.hanaro.hanaconnect.entity.Transaction transaction =
				com.hanaro.hanaconnect.entity.Transaction.builder()
					.transactionMoney(new BigDecimal("200000"))
					.transactionBalance(new BigDecimal(200000L * (i + 1)))
					.transactionType(TransactionType.DEPOSIT)
					.senderAccount(senderAccount)
					.receiverAccount(receiverAccount)
					.build();

			transaction.setCreatedAtForInit(paymentDate.atTime(12, 0));

			transactionRepository.save(transaction);
		}
	}
}
