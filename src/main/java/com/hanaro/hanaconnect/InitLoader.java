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
import com.hanaro.hanaconnect.common.security.AccountCryptoService;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Mission;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

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
	private final LinkedAccountRepository linkedAccountRepository;

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

		kid1 = memberRepository.save(kid1);
		parent1 = memberRepository.save(parent1);
		parent2 = memberRepository.save(parent2);

		Account kidAccount = accountRepository.save(createAccount(
			"아이 입출금 통장",
			"11122223333",
			"1234",
			AccountType.FREE,
			new BigDecimal("50000"),
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
			parent1
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

		accountRepository.save(createAccount(
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

		System.out.println("kid1 = " + kid1);
		System.out.println("parent1 = " + parent1);
		System.out.println("parent2 = " + parent2);

		relationRepository.save(createRelation(kid1, parent1));
		relationRepository.save(createRelation(kid1, parent2));

		relationRepository.save(createRelation(parent1, kid1));
		relationRepository.save(createRelation(parent1, parent2));

		relationRepository.save(createRelation(parent2, kid1));
		relationRepository.save(createRelation(parent2, parent1));

		phoneNameRepository.save(createPhoneName(kid1, parent1, "우리 엄마"));
		phoneNameRepository.save(createPhoneName(kid1, parent2, "외할머니"));
		phoneNameRepository.save(createPhoneName(parent1, parent2, "친정 엄마"));
		phoneNameRepository.save(createPhoneName(parent1, kid1, "우리 아들"));
		phoneNameRepository.save(createPhoneName(parent2, kid1, "손주"));
		phoneNameRepository.save(createPhoneName(parent2, parent1, "딸"));

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
				.name("식사 후 설거지 돕기")
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
}
