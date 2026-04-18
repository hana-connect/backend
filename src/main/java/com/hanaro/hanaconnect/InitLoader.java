package com.hanaro.hanaconnect;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.hanaro.hanaconnect.entity.Letter;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Mission;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.entity.Request;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.HouseRepository;
import com.hanaro.hanaconnect.repository.LetterRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.RequestRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;
import com.hanaro.hanaconnect.service.AccountHashService;

import io.swagger.v3.oas.models.links.Link;
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
	private final LetterRepository letterRepository;
	private final RequestRepository requestRepository;
	private final AccountHashService accountHashService;

	// 30
	List<String> parent1Messages = List.of(
		"우리 별돌이",
		"할머니가 늘",
		"사랑한단다",
		"차곡차곡 모아",
		"나중에 커서",
		"예쁜 집 짓자",
		"별돌이 방에는",
		"예쁜 창문도",
		"만들어 주렴",
		"할미의 큰 선물",
		"우리 별돌이",
		"오늘도 한 걸음",
		"조금씩 모아서",
		"꿈을 이루자",
		"반짝반짝 마음",
		"소중히 담아",
		"별처럼 빛나는",
		"우리 손주의",
		"예쁜 내일을",
		"응원한단다",
		"따뜻한 마음",
		"통장에 담아",
		"차곡차곡 쌓여",
		"행복이 되길",
		"웃음 가득한",
		"하루하루가",
		"멋진 추억으로",
		"남을 수 있게",
		"할머니가 늘",
		"곁에 있을게"
	);

	List<String> parent2Messages = List.of(
		"열심히 모아서",
		"별돌이 꿈을",
		"이루면 좋겠어",
		"무엇을 꿈꾸든",
		"엄마는 응원해",
		"사랑해 아들"
	);

	@Override
	@Transactional
	public void run(@Nullable ApplicationArguments args) {

		String encodedPassword = passwordEncoder.encode("123456");

		Member kid1 = createMember(
			"별돌이",
			encodedPassword,
			LocalDate.of(2013, 1, 2),
			"11122223333",
			MemberRole.KID
		);

		Member kid2 = createMember(
			"별송이",
			encodedPassword,
			LocalDate.of(2017, 8, 11),
			"22233334444",
			MemberRole.KID
		);


		Member parent1 = createMember(
			"할별돌",
			encodedPassword,
			LocalDate.of(1960, 5, 29),
			"99988887777",
			MemberRole.PARENT
		);

		Member parent2 = createMember(
			"별엄마",
			encodedPassword,
			LocalDate.of(1986, 12, 25),
			"88877776666",
			MemberRole.PARENT
		);


		kid1 = memberRepository.save(kid1);
		kid2 = memberRepository.save(kid2);
		parent1 = memberRepository.save(parent1);
		parent2 = memberRepository.save(parent2);

		// 별돌이 내 지갑 (가상계좌)
		Account kid1WalletAccount = accountRepository.save(createAccount(
			"별돌이 지갑",
			kid1.getVirtualAccount(), // member의 virtual account랑 같게
			"1234",
			AccountType.WALLET,
			new BigDecimal("500000"),
			new BigDecimal("2000000"),
			new BigDecimal("5000"),
			false,
			false,
			kid1,
			null
		));

		// 별돌이-할별돌, 별돌이-별엄마, 별돌이-별송이?
		// 반대 방향은 해당 멤버 영역에서
		relationRepository.save(createRelation(kid1, parent1));
		phoneNameRepository.save(createPhoneName(kid1, parent1, "할아버지"));
		relationRepository.save(createRelation(kid1, parent2));
		phoneNameRepository.save(createPhoneName(kid1, parent2, "우리 엄마"));
		relationRepository.save(createRelation(kid1, kid2));
		phoneNameRepository.save(createPhoneName(kid1, kid2, "동생"));


		// 별돌이 자유입출금
		// 걍 UI 보여주기용
		Account kid1FreeAccount = accountRepository.save(createAccount(
			"원픽 통장",
			"11122220000",
			"1234",
			AccountType.FREE,
			new BigDecimal("300000"),
			null,
			new BigDecimal("120000"),
			false,
			false,
			kid1,
			null
		));

		// 별돌이 적금
		// 적금 송금하기에서 사용할 메세지 적금
		Account kid1SavingsAccount = accountRepository.save(createAccount(
			"부자씨 적금",
			"11122224444",
			"1234",
			AccountType.SAVINGS,
			null,
			new BigDecimal("300000"),
			new BigDecimal("0"),
			false,
			false,
			kid1,
			null // parent1이랑 연결하고 싶은 linke account 테이블에 나오게
		));


		// 할별돌-별돌이 적금 계좌 연결.
		link(parent1, kid1SavingsAccount, "우리 큰 손주 별돌이 목돈");

		// 별돌이 청약
		// 시연할 때 청약 리포트 볼 청약 계좌
		Account kid1SubscriptionAccount = accountRepository.save(createAccount(
			"주택청약종합저축",
			"11122225555",
			"1234",
			AccountType.SUBSCRIPTION,
			null,
			null,
			BigDecimal.ZERO,
			false,
			false,
			kid1,
			null
		));

		// 별돌이 집 UI 생성용 더미 추가
		houseRepository.save(
			House.builder()
				.member(kid1)
				.account(kid1SubscriptionAccount)
				.level(7)
				.totalCount(83)
				.monthlyPayment(new BigDecimal("200000"))
				.startDate(LocalDate.of(2019, 5, 25)) // 84회 납입 날짜 맞추기
				.build()
		);


		// 할별돌-별돌이 청약 계좌 연결
		link(parent1, kid1SubscriptionAccount, "우리 큰 손주 별돌이 집 적금");


		// 별돌이 만기된 적금 계좌
		// 시연할 때 적금 우체통에서 볼 만기 적금 계좌
		Account kid1EndSavingsAccount = accountRepository.save(createAccount(
			"꿈꾸는 저금통",
			"11122224445",
			"1234",
			AccountType.SAVINGS,
			null,
			null,
			BigDecimal.ZERO, // 할아버지 10만원씩 30회 + 엄마 5만원씩 4회
			false,
			true,
			kid1,
			null // parent1이랑 연결하고 싶은 linke account 테이블에 나오게
		));


		// 별돌이 본인 계좌도 서비스에 등록
		link(kid1, kid1FreeAccount, "원픽 통장");
		link(kid1, kid1SavingsAccount, "부자씨 적금");
		link(kid1, kid1SubscriptionAccount, "주택청약종합저축");
		link(kid1, kid1EndSavingsAccount, "꿈꾸는 저금통");


		// 할별돌-별돌이 적금 계좌 연결
		link(parent1, kid1EndSavingsAccount, "별돌이의 꿈을 위해"); // 근데 메세지랑 안 맞는 것 같기도


		// 할별돌 내 지갑 (가상계좌)
		Account parent1WalletAccount = accountRepository.save(createAccount(
			"할별돌 지갑",
			parent1.getVirtualAccount(), // member의 virtual account랑 같게
			"9876",
			AccountType.WALLET,
			new BigDecimal("500000"),
			new BigDecimal("2000000"),
			new BigDecimal("5190000"),
			false,
			false,
			parent1,
			null
		));

		// 할별돌-별돌이, 할별돌-별엄마, 할별돌-별송이
		relationRepository.save(createRelation(parent1, kid1));
		phoneNameRepository.save(createPhoneName(parent1, kid1, "큰 손주"));
		relationRepository.save(createRelation(parent1, parent2));
		phoneNameRepository.save(createPhoneName(parent1, parent2, "며느리"));
		relationRepository.save(createRelation(parent1, kid2));
		phoneNameRepository.save(createPhoneName(parent1, kid2, "작은 손주"));

		// 별돌이 청약 히스토리 + 거래내역 추가
		createCheongyakTransactions(parent1WalletAccount, kid1SubscriptionAccount);

		// 할별돌 2만원 5번 + 3만원 3번 TODO 여기는 편지 안 씀?
		for (int i = 0; i < 5; i++) {
			createTransaction(
				parent1WalletAccount,
				kid1SavingsAccount,
				BigDecimal.valueOf(20000),
				TransactionType.SAVINGS_WITHDRAW,
				LocalDateTime.of(2025, 8, 7, 11, 0).plusMonths(i) // 달마다 납부
			);
		}

		for (int i = 0; i < 3; i++) {
			createTransaction(
				parent1WalletAccount,
				kid1SavingsAccount,
				BigDecimal.valueOf(30000),
				TransactionType.SAVINGS_WITHDRAW,
				LocalDateTime.of(2026, 1, 7, 11, 0).plusMonths(i)
			);
		}

		// 할별돌 예금
		// 시연할 때 할별돌이 등록할 본인 계좌
		// account 테이블에만 존재해야 함. LinkedAccount X
		Account parent1DepositAccount = accountRepository.save(createAccount(
			"369 정기예금",
			"99988886666",
			"9876",
			AccountType.DEPOSIT,
			null,
			null,
			new BigDecimal("25000000"),
			false,
			false,
			parent1,
			null
		));

		// 할별돌 연금계좌들과 자유입출금 계좌는 Linked Account 테이블에 등록되어 있어야 함
		// 할별돌 연금 계좌
		// 1. 개인형 IRP - 리워드
		Account parent1RewardAccount = accountRepository.save(createAccount(
			"개인형 IRP", // 이름을 그냥 이렇게 해도 될지?
			"99988885555",
			"9876",
			AccountType.PENSION,
			null,
			null,
			new BigDecimal("18000000"),
			true,
			false,
			parent1,
			null
		));

		// 2. 퇴직연금
		Account parent1PensionAccount1 = accountRepository.save(createAccount(
			"하나더넥스트 연금",
			"99988884444",
			"9876",
			AccountType.PENSION,
			null,
			null,
			new BigDecimal("140000000"),
			false,
			false,
			parent1,
			null
		));

		// 3. 연금 예금
		Account parent1PensionAccount2 = accountRepository.save(createAccount(
			"행복knowhow 연금", // 연금예금
			"99988883333",
			"9876",
			AccountType.PENSION,
			null,
			null,
			new BigDecimal("20000000"),
			false,
			false,
			parent1,
			null
		));

		// 할별돌 자유입출금
		Account parent1FreeAccount = accountRepository.save(createAccount(
			"주거래하나 통장",
			"99988882222",
			"9876",
			AccountType.FREE,
			null,
			null,
			new BigDecimal("8600000"),
			false,
			false,
			parent1,
			null
		));

		// 할별돌 본인 계좌도 서비스에 등록
		link(parent1, parent1FreeAccount, "주거래하나 통장");
		link(parent1, parent1RewardAccount, "개인형 IRP");
		link(parent1, parent1PensionAccount1, "하나더넥스트 연금통장");
		link(parent1, parent1PensionAccount2, "행복knowhow 연금예금");

		// Linked Account 등록된 아이 계좌. 위에 TODO랑 겹치는 듯?
		// 1. 별돌이 청약
		// 2. 별돌이 적금
		// 3. 만기된 별돌이 적금

		// 별송이 청약 별송이 파트에 있음

		// 별엄마 내 지갑 (가상계좌)
		Account parent2WalletAccount = accountRepository.save(createAccount(
			"별엄마 지갑",
			parent2.getVirtualAccount(), // member의 virtual account랑 같게
			"9876",
			AccountType.WALLET,
			new BigDecimal("500000"),
			new BigDecimal("2000000"),
			new BigDecimal("1200000"),
			false,
			false,
			parent2,
			null
		));

		// 별엄마-별돌이, 별엄마-할별돌, 별엄마-별송이
		relationRepository.save(createRelation(parent2, kid1));
		phoneNameRepository.save(createPhoneName(parent2, kid1, "우리 아들"));
		relationRepository.save(createRelation(parent2, parent1));
		phoneNameRepository.save(createPhoneName(parent2, parent1, "아버님"));
		relationRepository.save(createRelation(parent2, kid2));
		phoneNameRepository.save(createPhoneName(parent2, kid2, "우리 딸"));

		// 별엄마-별돌이 적금 계좌 연결
		link(parent2, kid1EndSavingsAccount, "돌이 꿈 응원 적금");

		// 별돌이 만기 적금 송금 이력
		for (int i = 0; i < parent1Messages.size(); i++) {
			createSavingsWithLetter(
				parent1WalletAccount,
				kid1EndSavingsAccount,
				BigDecimal.valueOf(100000),
				parent1Messages.get(i),
				LocalDateTime.of(2023, 1, 10, 10, 0).plusMonths(i)
			);
		}

		for (int i = 0; i < parent2Messages.size(); i++) {
			createSavingsWithLetter(
				parent2WalletAccount,
				kid1EndSavingsAccount,
				BigDecimal.valueOf(50000),
				parent2Messages.get(i),
				LocalDateTime.of(2025, 7, 5, 12, 0).plusMonths(i)
			);
		}


		// 별엄마 자유입출금
		Account parent2FreeAccount = accountRepository.save(createAccount(
			"주거래하나 통장",
			"8887775555",
			"8765",
			AccountType.FREE,
			null,
			null,
			new BigDecimal("3420000"),
			false,
			false,
			parent2,
			null
		));

		// 별엄마 본인 계좌도 서비스에 등록
		link(parent2, parent2FreeAccount, "주거래하나 통장");

		// 별송이 내 지갑 (가상계좌)
		Account kid2WalletAccount = accountRepository.save(createAccount(
			"별송이 지갑",
			kid2.getVirtualAccount(), // member의 virtual account랑 같게
			"2345",
			AccountType.WALLET,
			new BigDecimal("500000"),
			new BigDecimal("2000000"),
			new BigDecimal("12000"),
			false,
			false,
			kid2,
			null
		));

		// 별송이-별돌이, 별송이-할별돌, 별송이-별엄마
		relationRepository.save(createRelation(kid2, kid1));
		phoneNameRepository.save(createPhoneName(kid2, kid1, "오빠"));
		relationRepository.save(createRelation(kid2, parent1));
		phoneNameRepository.save(createPhoneName(kid2, parent1, "할아버지"));
		relationRepository.save(createRelation(kid2, parent2));
		phoneNameRepository.save(createPhoneName(kid2, parent2, "우리 엄마"));


		// 별송이 청약
		// 시연할 때 할별돌이 등록할 별송이 계좌
		// account 테이블에만 존재해야 함. LinkedAccount X
		Account kid2SubscriptionAccount = accountRepository.save(createAccount(
			"주택청약종합저축",
			"22233335555",
			"2345",
			AccountType.SUBSCRIPTION,
			null,
			null,
			BigDecimal.ZERO, // 계좌 처음 연결이므로 납부액 0
			false,
			false,
			kid2,
			null
		));

		// 할별돌용 별돌이 미션 퀴즈
		createSampleMissions(kid1, parent1);

		// request 테이블에 더미 넣기
		requestRepository.save(
			Request.builder()
				.content("별송이 청약 넣게 계좌번호 좀 알려다오")
				.sender(parent1)
				.receiver(parent2)
				.build()
		);

	}

	// 유저 생성 헬퍼
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
			.memberRole(memberRole)
			.role(Role.USER)
			.build();
	}

	// 계좌(기존 하나은행) 생성 헬퍼
	private Account createAccount(
		String name,
		String rawAccountNumber,
		String rawPassword,
		AccountType accountType,
		BigDecimal dailyLimit,
		BigDecimal totalLimit,
		BigDecimal balance,
		Boolean isReward,
		Boolean isEnd,
		Member member,
		List<LinkedAccount> linkedMembers
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
			.dailyLimit(dailyLimit)
			.totalLimit(totalLimit)
			.balance(balance)
			.isReward(isReward)
			.isEnd(isEnd)
			.member(member)
			.linkedMembers(linkedMembers)
			.build();
	}

	// 관계 연결 헬퍼
	private void link(Member member, Account account, String nickname) {
		linkedAccountRepository.save(
			LinkedAccount.builder()
				.member(member)
				.account(account)
				.nickname(nickname)
				.build()
		);
	}

	// 관계 생성 헬퍼
	private Relation createRelation(Member member, Member connectMember) {
		return Relation.builder()
			.member(member)
			.connectMember(connectMember)
			.connectMemberRole(connectMember.getMemberRole())
			.build();
	}

	// 휴대폰 저장 이름 생성 헬퍼
	private PhoneName createPhoneName(Member who, Member whom, String whomName) {
		return PhoneName.builder()
			.who(who)
			.whom(whom)
			.whomName(whomName)
			.build();
	}

	// 미션 생성 헬퍼
	private void createSampleMissions(Member kid, Member parent) {
		missionRepository.saveAll(List.of(
			Mission.builder().kid(kid).parent(parent).name("부모님께 인사하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("심부름 다녀오기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("용돈 기록 작성하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("방 정리하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("오늘 소비 내역 확인하기").isCompleted(true).build()
		));
	}

	// 거래 이력 생성 헬퍼
	private void createTransaction(
		Account sender,
		Account receiver,
		BigDecimal amount,
		TransactionType type,
		LocalDateTime createdAt
	) {
		if (type == TransactionType.SUBSCRIPTION) {
			sender.withdraw(amount);
			receiver.deposit(amount);

			Transaction tx = Transaction.builder()
				.transactionMoney(amount)
				.transactionBalance(receiver.getBalance())
				.transactionType(TransactionType.SUBSCRIPTION)
				.senderAccount(sender)
				.receiverAccount(receiver)
				.build();

			tx.setCreatedAtForInit(createdAt);
			transactionRepository.save(tx);
			return;
		}

		if (type == TransactionType.SAVINGS_WITHDRAW) {
			sender.withdraw(amount);
			receiver.deposit(amount);

			Transaction withdrawTx = Transaction.builder()
				.transactionMoney(amount)
				.transactionBalance(sender.getBalance())
				.transactionType(TransactionType.SAVINGS_WITHDRAW)
				.senderAccount(sender)
				.receiverAccount(receiver)
				.build();
			withdrawTx.setCreatedAtForInit(createdAt);

			Transaction depositTx = Transaction.builder()
				.transactionMoney(amount)
				.transactionBalance(receiver.getBalance())
				.transactionType(TransactionType.SAVINGS_DEPOSIT)
				.senderAccount(sender)
				.receiverAccount(receiver)
				.build();
			depositTx.setCreatedAtForInit(createdAt);

			transactionRepository.save(withdrawTx);
			transactionRepository.save(depositTx);
			return;
		}

		if (type == TransactionType.WITHDRAW) {
			sender.withdraw(amount);
			receiver.deposit(amount);

			Transaction withdrawTx = Transaction.builder()
				.transactionMoney(amount)
				.transactionBalance(sender.getBalance())
				.transactionType(TransactionType.WITHDRAW)
				.senderAccount(sender)
				.receiverAccount(receiver)
				.build();
			withdrawTx.setCreatedAtForInit(createdAt);

			Transaction depositTx = Transaction.builder()
				.transactionMoney(amount)
				.transactionBalance(receiver.getBalance())
				.transactionType(TransactionType.DEPOSIT)
				.senderAccount(sender)
				.receiverAccount(receiver)
				.build();
			depositTx.setCreatedAtForInit(createdAt);

			transactionRepository.save(withdrawTx);
			transactionRepository.save(depositTx);
			return;
		}

		throw new IllegalArgumentException("지원하지 않는 거래 타입입니다.");
	}

	// 거래 + 편지 생성 헬퍼
	private void createSavingsWithLetter(
		Account sender,
		Account receiver,
		BigDecimal amount,
		String message,
		LocalDateTime createdAt
	) {
		sender.withdraw(amount);
		receiver.deposit(amount);

		Transaction withdrawTx = Transaction.builder()
			.transactionMoney(amount)
			.transactionBalance(sender.getBalance())
			.transactionType(TransactionType.SAVINGS_WITHDRAW)
			.senderAccount(sender)
			.receiverAccount(receiver)
			.build();
		withdrawTx.setCreatedAtForInit(createdAt);

		Transaction depositTx = Transaction.builder()
			.transactionMoney(amount)
			.transactionBalance(receiver.getBalance())
			.transactionType(TransactionType.SAVINGS_DEPOSIT)
			.senderAccount(sender)
			.receiverAccount(receiver)
			.build();
		depositTx.setCreatedAtForInit(createdAt);

		transactionRepository.save(withdrawTx);
		Transaction savedDepositTx = transactionRepository.save(depositTx);

		String normalizedMessage = (message == null) ? null : message.trim();
		if (normalizedMessage != null && !normalizedMessage.isEmpty()) {
			Letter letter = Letter.builder()
				.content(normalizedMessage)
				.transaction(savedDepositTx)
				.build();

			letterRepository.save(letter);
		}
	}

	// 청약 히스토리 생성 헬퍼
	private void createCheongyakTransactions(Account senderAccount, Account receiverAccount) {
		LocalDateTime startDate = LocalDateTime.of(2019, 4, 15, 16, 30);
		BigDecimal amount = new BigDecimal("200000"); // 매달 20만원씩 납부

		for (int i = 0; i < 84; i++) {
			LocalDateTime paymentDateTime = startDate.plusMonths(i);

			Transaction tx = Transaction.builder()
				.transactionMoney(amount)
				.transactionBalance(receiverAccount.getBalance())
				.transactionType(TransactionType.SUBSCRIPTION)
				.senderAccount(senderAccount)
				.receiverAccount(receiverAccount)
				.build();

			tx.setCreatedAtForInit(paymentDateTime); // 날짜 수정

			transactionRepository.save(tx);
		}
	}

}
