package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import com.hanaro.hanaconnect.dto.saving.RelayResponseDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsDetailResponseDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.transfer.RecentTransferResponseDTO;
import com.hanaro.hanaconnect.dto.transfer.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.dto.transfer.TransferRequestDto;
import com.hanaro.hanaconnect.dto.transfer.TransferResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Letter;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LetterRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class TransferServiceTest {

	@Autowired
	private TransferService transferService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private LinkedAccountRepository linkedAccountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private LetterRepository letterRepository;

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private AccountCryptoService accountCryptoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccountHashService accountHashService;

	private static long accountSeq = 88000000000L;

	private Member parent;
	private Member kid;
	private Member stranger;

	private Account parentWalletAccount;
	private Account kidSavingsAccount;
	private Account kidCheckingAccount;
	private Account strangerFreeAccount;

	private Account expiredSavingsAccount;

	@BeforeEach
	void setUp() {
		parent = createMember("김엄마", LocalDate.of(1980, 5, 19), MemberRole.PARENT);
		kid = createMember("홍길동", LocalDate.of(2010, 1, 2), MemberRole.KID);
		stranger = createMember("모르는사람", LocalDate.of(1985, 3, 1), MemberRole.PARENT);

		saveRelation(parent, kid);
		saveRelation(kid, parent);

		parentWalletAccount = createAccount(
			"김엄마 지갑",
			"260420",
			AccountType.WALLET,
			new BigDecimal("800000"),
			parent,
			BigDecimal.ZERO,
			false,
			false
		);

		kidSavingsAccount = createAccount(
			"홍길동 적금",
			"1234",
			AccountType.SAVINGS,
			new BigDecimal("250000"),
			kid,
			new BigDecimal("300000"),
			false,
			false
		);

		kidCheckingAccount = createAccount(
			"홍길동 입출금",
			"1234",
			AccountType.FREE,
			new BigDecimal("50000"),
			kid,
			BigDecimal.ZERO,
			false,
			false
		);

		strangerFreeAccount = createAccount(
			"모르는사람 통장",
			"9999",
			AccountType.FREE,
			new BigDecimal("100000"),
			stranger,
			BigDecimal.ZERO,
			false,
			false
		);

		expiredSavingsAccount = createAccount(
			"만기 적금",
			"1234",
			AccountType.SAVINGS,
			new BigDecimal("2000000"),
			kid,
			new BigDecimal("300000"),
			false,
			true
		);

		link(parent, kidSavingsAccount);
		link(parent, kidCheckingAccount);
		link(kid, kidSavingsAccount);
		link(kid, kidCheckingAccount);
		link(kid, expiredSavingsAccount);

		createSavingsDepositTransaction(
			parentWalletAccount,
			expiredSavingsAccount,
			new BigDecimal("10000"),
			new BigDecimal("2010000"),
			LocalDateTime.of(2026, 4, 1, 12, 0),
			"만기 전에 보낸 편지"
		);
	}

	@Test
	@DisplayName("적금 송금 성공")
	void transferToChildSavingsSuccessTest() {
		BigDecimal senderBefore = parentWalletAccount.getBalance();
		BigDecimal receiverBefore = kidSavingsAccount.getBalance();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setPassword("260420");
		request.setContent("적금 응원 편지");

		SavingsTransferResponseDTO result =
			transferService.transferToChildSavings(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getTransactionMoney()).isEqualByComparingTo("10000");
		assertThat(result.getMessage()).isEqualTo("적금 응원 편지");

		Account updatedSender = accountRepository.findById(parentWalletAccount.getId()).orElseThrow();
		Account updatedReceiver = accountRepository.findById(kidSavingsAccount.getId()).orElseThrow();

		assertThat(updatedSender.getBalance())
			.isEqualByComparingTo(senderBefore.subtract(new BigDecimal("10000")));
		assertThat(updatedReceiver.getBalance())
			.isEqualByComparingTo(receiverBefore.add(new BigDecimal("10000")));

		List<Letter> letters = letterRepository.findAll();
		assertThat(letters).anyMatch(letter -> "적금 응원 편지".equals(letter.getContent()));
	}

	@Test
	@DisplayName("적금 송금 성공 - 메시지가 공백이면 편지를 저장하지 않는다")
	void transferToChildSavingsSuccessWithoutLetter() {
		BigDecimal senderBefore = parentWalletAccount.getBalance();
		BigDecimal receiverBefore = kidSavingsAccount.getBalance();
		long beforeLetterCount = letterRepository.count();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setPassword("260420");
		request.setContent("   ");

		SavingsTransferResponseDTO result =
			transferService.transferToChildSavings(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getTransactionMoney()).isEqualByComparingTo("10000");
		assertThat(result.getMessage()).isBlank();
		assertThat(letterRepository.count()).isEqualTo(beforeLetterCount);

		Account updatedSender = accountRepository.findById(parentWalletAccount.getId()).orElseThrow();
		Account updatedReceiver = accountRepository.findById(kidSavingsAccount.getId()).orElseThrow();

		assertThat(updatedSender.getBalance())
			.isEqualByComparingTo(senderBefore.subtract(new BigDecimal("10000")));
		assertThat(updatedReceiver.getBalance())
			.isEqualByComparingTo(receiverBefore.add(new BigDecimal("10000")));
	}

	@Test
	@DisplayName("적금 송금 성공 - 메시지가 null이면 편지를 저장하지 않는다")
	void transferToChildSavingsSuccessWithNullContent() {
		BigDecimal senderBefore = parentWalletAccount.getBalance();
		BigDecimal receiverBefore = kidSavingsAccount.getBalance();
		long beforeLetterCount = letterRepository.count();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("7000"));
		request.setPassword("260420");
		request.setContent(null);

		SavingsTransferResponseDTO result =
			transferService.transferToChildSavings(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getTransactionMoney()).isEqualByComparingTo("7000");
		assertThat(result.getMessage()).isNull();
		assertThat(letterRepository.count()).isEqualTo(beforeLetterCount);

		Account updatedSender = accountRepository.findById(parentWalletAccount.getId()).orElseThrow();
		Account updatedReceiver = accountRepository.findById(kidSavingsAccount.getId()).orElseThrow();

		assertThat(updatedSender.getBalance())
			.isEqualByComparingTo(senderBefore.subtract(new BigDecimal("7000")));
		assertThat(updatedReceiver.getBalance())
			.isEqualByComparingTo(receiverBefore.add(new BigDecimal("7000")));
	}

	@Test
	@DisplayName("적금 송금 실패 - 비밀번호 불일치")
	void transferToChildSavingsFailWrongPasswordTest() {
		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setPassword("000000");
		request.setContent("편지");

		assertThatThrownBy(() -> transferService.transferToChildSavings(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("비밀번호가 일치하지 않습니다.");
	}

	@Test
	@DisplayName("적금 송금 실패 - 연결되지 않은 손주 계좌")
	void transferToChildSavingsFailUnlinkedAccountTest() {
		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(strangerFreeAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setPassword("260420");
		request.setContent("편지");

		assertThatThrownBy(() -> transferService.transferToChildSavings(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("연결된 손주 계좌가 아닙니다.");
	}

	@Test
	@DisplayName("일반 송금 성공")
	void transferSuccess() {
		BigDecimal parentBefore = parentWalletAccount.getBalance();
		BigDecimal kidBefore = kidCheckingAccount.getBalance();

		TransferRequestDto request = new TransferRequestDto();
		request.setAccountId(kidCheckingAccount.getId());
		request.setAmount(new BigDecimal("5000"));
		request.setPassword("260420");

		TransferResponseDto result = transferService.transfer(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getAmount()).isEqualByComparingTo("5000");
		assertThat(result.getTransferredAt()).isNotNull();

		String rawAccountNumber =
			accountCryptoService.decrypt(kidCheckingAccount.getAccountNumber()).replaceAll("-", "");
		String resultAccountNumber =
			result.getToAccountNumber().replaceAll("-", "");

		assertThat(resultAccountNumber).isEqualTo(rawAccountNumber);

		Account updatedParent = accountRepository.findById(parentWalletAccount.getId()).orElseThrow();
		Account updatedKid = accountRepository.findById(kidCheckingAccount.getId()).orElseThrow();

		assertThat(updatedParent.getBalance()).isEqualByComparingTo(parentBefore.subtract(new BigDecimal("5000")));
		assertThat(updatedKid.getBalance()).isEqualByComparingTo(kidBefore.add(new BigDecimal("5000")));
	}

	@Test
	@DisplayName("일반 송금 실패 - 비밀번호 불일치")
	void transferFailWrongPassword() {
		TransferRequestDto request = new TransferRequestDto();
		request.setAccountId(kidCheckingAccount.getId());
		request.setAmount(new BigDecimal("5000"));
		request.setPassword("000000");

		assertThatThrownBy(() -> transferService.transfer(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("비밀번호가 일치하지 않습니다.");
	}

	@Test
	@DisplayName("일반 송금 실패 - 관계 없는 계좌")
	void transferFailNoRelation() {
		TransferRequestDto request = new TransferRequestDto();
		request.setAccountId(strangerFreeAccount.getId());
		request.setAmount(new BigDecimal("5000"));
		request.setPassword("260420");

		assertThatThrownBy(() -> transferService.transfer(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("송금 사전 정보 조회 성공 - 적금 계좌")
	void getTransferPrepareInfoSavingsSuccess() {
		TransferPrepareResponseDto result =
			transferService.getTransferPrepareInfo(parent.getId(), kidSavingsAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getAccountId()).isEqualTo(kidSavingsAccount.getId());
		assertThat(result.getTargetMemberName()).isEqualTo(kid.getName());
		assertThat(result.getAccountAlias()).isEqualTo(kidSavingsAccount.getName());
		assertThat(result.getBalance()).isEqualByComparingTo(parentWalletAccount.getBalance());
		assertThat(result.getCurrentSaving()).isEqualByComparingTo(kidSavingsAccount.getBalance());
		assertThat(result.getSavingLimit()).isEqualByComparingTo(kidSavingsAccount.getTotalLimit());
	}

	@Test
	@DisplayName("송금 사전 정보 조회 성공 - 일반 계좌")
	void getTransferPrepareInfoNonSavingsSuccess() {
		TransferPrepareResponseDto result =
			transferService.getTransferPrepareInfo(parent.getId(), kidCheckingAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getAccountId()).isEqualTo(kidCheckingAccount.getId());
		assertThat(result.getBalance()).isEqualByComparingTo(parentWalletAccount.getBalance());
		assertThat(result.getCurrentSaving()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.getSavingLimit()).isNotNull();
	}

	@Test
	@DisplayName("송금 사전 정보 조회 실패 - 존재하지 않는 계좌")
	void getTransferPrepareInfoFailNoAccount() {
		assertThatThrownBy(() -> transferService.getTransferPrepareInfo(parent.getId(), 999999L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("존재하지 않는 계좌입니다.");
	}

	@Test
	@DisplayName("송금 사전 정보 조회 실패 - 관계 없는 계좌")
	void getTransferPrepareInfoFailNoRelation() {
		assertThatThrownBy(() -> transferService.getTransferPrepareInfo(parent.getId(), strangerFreeAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("최근 적금 송금 내역 조회 성공")
	void getRecentTransferAmountSuccess() {
		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("12000"));
		request.setPassword("260420");
		request.setContent("최근 송금 테스트");

		transferService.transferToChildSavings(parent.getId(), request);

		RecentTransferResponseDTO result =
			transferService.getRecentTransferAmount(parent.getId(), kidSavingsAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getAmount()).isEqualByComparingTo("12000");
		assertThat(result.getTransactionDate()).isNotNull();
	}

	@Test
	@DisplayName("최근 적금 송금 내역 조회 실패 - 관계 없는 계좌")
	void getRecentTransferAmountFailNoRelation() {
		assertThatThrownBy(() -> transferService.getRecentTransferAmount(parent.getId(), strangerFreeAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("적금 릴레이 내역 조회 성공")
	void getRelayHistorySuccessTest() {
		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("50000"));
		request.setPassword("260420");
		request.setContent("할머니가 주는 용돈이다!");

		transferService.transferToChildSavings(parent.getId(), request);

		RelayResponseDTO result =
			transferService.getRelayHistory(parent.getId(), kidSavingsAccount.getId(), 0);

		assertThat(result).isNotNull();
		assertThat(result.getProductNickname()).isEqualTo(kidSavingsAccount.getName());

		String rawAccountNumber = accountCryptoService.decrypt(kidSavingsAccount.getAccountNumber());
		assertThat(result.getAccountNumber().replaceAll("-", ""))
			.isEqualTo(rawAccountNumber.replaceAll("-", ""));

		assertThat(result.getHistory()).isNotEmpty();
		assertThat(result.getHistory().get(0).getMessage()).isEqualTo("할머니가 주는 용돈이다!");
		assertThat(result.getHistory().get(0).getAmount()).isEqualByComparingTo("50000");
	}

	@Test
	@DisplayName("적금 릴레이 내역 조회 실패 케이스 모음")
	void getRelayHistoryFailTest() {
		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), 999L, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");

		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), strangerFreeAccount.getId(), 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");

		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), kidCheckingAccount.getId(), 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("적금 계좌만 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("최근 적금 릴레이 내역 조회 성공")
	void getRecentRelayHistorySuccess() {
		SavingsTransferRequestDTO request1 = new SavingsTransferRequestDTO();
		request1.setTargetAccountId(kidSavingsAccount.getId());
		request1.setAmount(new BigDecimal("1000"));
		request1.setPassword("260420");
		request1.setContent("첫 번째");

		SavingsTransferRequestDTO request2 = new SavingsTransferRequestDTO();
		request2.setTargetAccountId(kidSavingsAccount.getId());
		request2.setAmount(new BigDecimal("2000"));
		request2.setPassword("260420");
		request2.setContent("두 번째");

		transferService.transferToChildSavings(parent.getId(), request1);
		transferService.transferToChildSavings(parent.getId(), request2);

		RelayResponseDTO result =
			transferService.getRecentRelayHistory(parent.getId(), kidSavingsAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getProductNickname()).isEqualTo(kidSavingsAccount.getName());

		String rawAccountNumber = accountCryptoService.decrypt(kidSavingsAccount.getAccountNumber()).replaceAll("-", "");
		String resultAccountNumber = result.getAccountNumber().replaceAll("-", "");

		assertThat(resultAccountNumber).isEqualTo(rawAccountNumber);

		assertThat(result.getHistory()).isNotNull();
		assertThat(result.getHistory().size()).isLessThanOrEqualTo(3);
	}

	@Test
	@DisplayName("최근 적금 릴레이 내역 조회 실패 - 적금 계좌 아님")
	void getRecentRelayHistoryFailNotSavings() {
		assertThatThrownBy(() -> transferService.getRecentRelayHistory(parent.getId(), kidCheckingAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("적금 계좌만 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 성공 - 전체 조회")
	void getExpiredSavingsDetail_Success() {
		Long ownerId = kid.getId();

		SavingsDetailResponseDTO result =
			transferService.getExpiredSavingsDetail(ownerId, expiredSavingsAccount.getId(), 0, null);

		assertThat(result).isNotNull();
		assertThat(result.getSenders()).isNotNull();
		assertThat(result.getProductName()).isNotBlank();

		String rawAccountNumber =
			accountCryptoService.decrypt(expiredSavingsAccount.getAccountNumber()).replaceAll("-", "");
		String resultAccountNumber =
			result.getAccountNumber().replaceAll("-", "");

		assertThat(resultAccountNumber).isEqualTo(rawAccountNumber);
		assertThat(result.getTransactions()).isNotNull();
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 성공 - 특정 발신인 필터링")
	void getExpiredSavingsDetail_Filter_Success() {
		SavingsDetailResponseDTO unfiltered =
			transferService.getExpiredSavingsDetail(kid.getId(), expiredSavingsAccount.getId(), 0, null);

		assertThat(unfiltered.getSenders()).isNotEmpty();

		Long specificSenderId = unfiltered.getSenders().get(0).getSenderId();

		SavingsDetailResponseDTO result =
			transferService.getExpiredSavingsDetail(kid.getId(), expiredSavingsAccount.getId(), 0, specificSenderId);

		assertThat(result).isNotNull();
		assertThat(result.getTransactions()).isNotEmpty();

		result.getTransactions().forEach(txData ->
			assertThat(txData.getSenderId()).isEqualTo(specificSenderId)
		);
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 권한 없음")
	void getExpiredSavingsDetail_Fail_NotOwner() {
		assertThatThrownBy(() -> transferService.getExpiredSavingsDetail(parent.getId(), expiredSavingsAccount.getId(), 0, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("본인의 계좌만 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 만기되지 않은 계좌")
	void getExpiredSavingsDetail_Fail_NotExpired() {
		assertThatThrownBy(() -> transferService.getExpiredSavingsDetail(kid.getId(), kidSavingsAccount.getId(), 0, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("만기된 계좌만 상세 조회가 가능합니다.");
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 계좌 없음")
	void getExpiredSavingsDetail_Fail_NoAccount() {
		assertThatThrownBy(() -> transferService.getExpiredSavingsDetail(kid.getId(), 999999L, 0, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("계좌를 찾을 수 없습니다.");
	}

	private Member createMember(String name, LocalDate birthday, MemberRole memberRole) {
		String rawVirtualAccount = generateAccountNumber();

		return memberRepository.save(
			Member.builder()
				.name(name)
				.password(passwordEncoder.encode("260420"))
				.birthday(birthday)
				.virtualAccount(accountCryptoService.encrypt(rawVirtualAccount))
				.memberRole(memberRole)
				.role(Role.USER)
				.build()
		);
	}

	private void saveRelation(Member member, Member connectMember) {
		Relation relation = Relation.builder()
			.member(member)
			.connectMember(connectMember)
			.connectMemberRole(connectMember.getMemberRole())
			.build();

		relationRepository.save(relation);
	}

	private Account createAccount(
		String name,
		String rawPassword,
		AccountType accountType,
		BigDecimal balance,
		Member member,
		BigDecimal totalLimit,
		boolean isReward,
		boolean isEnd
	) {
		String rawAccountNumber = generateAccountNumber();

		return accountRepository.save(
			Account.builder()
				.name(name)
				.accountNumber(accountCryptoService.encrypt(rawAccountNumber))
				.accountNumberHash(accountHashService.hash(rawAccountNumber))
				.password(passwordEncoder.encode(rawPassword))
				.accountType(accountType)
				.balance(balance)
				.member(member)
				.totalLimit(totalLimit)
				.isReward(isReward)
				.isEnd(isEnd)
				.build()
		);
	}

	private void link(Member member, Account account) {
		LinkedAccount linkedAccount = LinkedAccount.builder()
			.member(member)
			.account(account)
			.build();

		linkedAccount.setCreatedAtForInit(LocalDateTime.of(2026, 4, 10, 12, 0));
		linkedAccountRepository.save(linkedAccount);
	}

	private void createSavingsDepositTransaction(
		Account sender,
		Account receiver,
		BigDecimal amount,
		BigDecimal balanceAfter,
		LocalDateTime createdAt,
		String message
	) {
		Transaction tx = Transaction.builder()
			.senderAccount(sender)
			.receiverAccount(receiver)
			.transactionMoney(amount)
			.transactionBalance(balanceAfter)
			.transactionType(TransactionType.SAVINGS_DEPOSIT)
			.build();

		tx.setCreatedAtForInit(createdAt);
		transactionRepository.save(tx);

		if (message != null && !message.isBlank()) {
			letterRepository.save(
				Letter.builder()
					.content(message)
					.transaction(tx)
					.build()
			);
		}
	}

	private String generateAccountNumber() {
		return String.valueOf(accountSeq++);
	}
}
