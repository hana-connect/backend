package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.transfer.RecentTransferResponseDTO;
import com.hanaro.hanaconnect.dto.saving.RelayResponseDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsDetailResponseDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.transfer.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.dto.transfer.TransferRequestDto;
import com.hanaro.hanaconnect.dto.transfer.TransferResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Letter;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
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
	private AccountCryptoService accountCryptoService;

	@Autowired
	private RelationRepository relationRepository;

	private Member findParent() {
		return memberRepository.findAll().stream()
			.filter(member -> "김엄마".equals(member.getName()))
			.filter(member -> member.getMemberRole() == MemberRole.PARENT)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 부모 회원(김엄마)을 찾을 수 없습니다."));
	}

	private Account findParentFreeAccount(Long parentId) {
		return accountRepository.findAll().stream()
			.filter(account -> account.getMember().getId().equals(parentId))
			.filter(account -> account.getAccountType() == AccountType.FREE)
			.filter(account -> !Boolean.TRUE.equals(account.getIsReward()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 부모 FREE 계좌를 찾을 수 없습니다."));
	}

	private Account findLinkedKidSavingsAccount(Long parentId) {
		return accountRepository.findAll().stream()
			.filter(account -> account.getAccountType() == AccountType.SAVINGS)
			.filter(account -> linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), parentId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 연결된 아이 적금 계좌를 찾을 수 없습니다."));
	}

	private Account findLinkedKidCheckingAccount(Long memberId) {
		return linkedAccountRepository.findAllByMemberId(memberId).stream()
			.map(LinkedAccount::getAccount)
			.filter(a -> a.getAccountType() == AccountType.FREE)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 연결된 일반 계좌를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("적금 송금 성공")
	void transferToChildSavingsSuccessTest() {
		Member parent = findParent();
		Account parentFreeAccount = findParentFreeAccount(parent.getId());
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		BigDecimal senderBefore = parentFreeAccount.getBalance();
		BigDecimal receiverBefore = kidSavingsAccount.getBalance();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setPassword("123456");
		request.setContent("적금 응원 편지");

		SavingsTransferResponseDTO result =
			transferService.transferToChildSavings(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getTransactionMoney()).isEqualByComparingTo("10000");
		assertThat(result.getMessage()).isEqualTo("적금 응원 편지");

		Account updatedSender = accountRepository.findById(parentFreeAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("부모 계좌를 다시 찾을 수 없습니다."));
		Account updatedReceiver = accountRepository.findById(kidSavingsAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("아이 적금 계좌를 다시 찾을 수 없습니다."));

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
		Member parent = findParent();
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		long beforeLetterCount = letterRepository.count();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setPassword("123456");
		request.setContent("   ");

		SavingsTransferResponseDTO result =
			transferService.transferToChildSavings(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getTransactionMoney()).isEqualByComparingTo("10000");
		assertThat(result.getMessage()).isBlank();
		assertThat(letterRepository.count()).isEqualTo(beforeLetterCount);
	}

	@Test
	@DisplayName("적금 송금 성공 - 메시지가 null이면 편지를 저장하지 않는다")
	void transferToChildSavingsSuccessWithNullContent() {
		Member parent = findParent();
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		long beforeLetterCount = letterRepository.count();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("7000"));
		request.setPassword("123456");
		request.setContent(null);

		SavingsTransferResponseDTO result =
			transferService.transferToChildSavings(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getTransactionMoney()).isEqualByComparingTo("7000");
		assertThat(result.getMessage()).isNull();
		assertThat(letterRepository.count()).isEqualTo(beforeLetterCount);
	}

	@Test
	@DisplayName("적금 송금 실패 - 비밀번호 불일치")
	void transferToChildSavingsFailWrongPasswordTest() {
		Member parent = findParent();
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

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
		Member parent = findParent();
		Long unlinkedId = findUnlinkedAccountId(parent.getId());

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(unlinkedId);
		request.setAmount(new BigDecimal("10000"));
		request.setPassword("123456");
		request.setContent("편지");

		assertThatThrownBy(() -> transferService.transferToChildSavings(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("연결된 손주 계좌가 아닙니다.");
	}

	@Test
	@DisplayName("일반 송금 성공")
	void transferSuccess() {
		Member parent = findParent();

		Account parentFree = findParentFreeAccount(parent.getId());
		Account targetKidAccount = findLinkedKidCheckingAccount(parent.getId());

		BigDecimal parentBefore = parentFree.getBalance();
		BigDecimal kidBefore = targetKidAccount.getBalance();

		TransferRequestDto request = new TransferRequestDto();
		request.setAccountId(targetKidAccount.getId());
		request.setAmount(new BigDecimal("5000"));
		request.setPassword("123456");

		TransferResponseDto result = transferService.transfer(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getAmount()).isEqualByComparingTo("5000");
		assertThat(result.getTransferredAt()).isNotNull();

		String rawAccountNumber =
			accountCryptoService.decrypt(targetKidAccount.getAccountNumber()).replaceAll("-", "");
		String resultAccountNumber =
			result.getToAccountNumber().replaceAll("-", "");

		assertThat(resultAccountNumber).isEqualTo(rawAccountNumber);

		assertThat(resultAccountNumber).isEqualTo(rawAccountNumber);

		Account updatedParent = accountRepository.findById(parentFree.getId()).orElseThrow();
		Account updatedKid = accountRepository.findById(targetKidAccount.getId()).orElseThrow();

		assertThat(updatedParent.getBalance()).isEqualByComparingTo(parentBefore.subtract(new BigDecimal("5000")));
		assertThat(updatedKid.getBalance()).isEqualByComparingTo(kidBefore.add(new BigDecimal("5000")));
	}

	@Test
	@DisplayName("일반 송금 실패 - 비밀번호 불일치")
	void transferFailWrongPassword() {
		Member parent = findParent();
		Account targetKidAccount = findLinkedKidCheckingAccount(parent.getId());

		TransferRequestDto request = new TransferRequestDto();
		request.setAccountId(targetKidAccount.getId());
		request.setAmount(new BigDecimal("5000"));
		request.setPassword("000000");

		assertThatThrownBy(() -> transferService.transfer(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("비밀번호가 일치하지 않습니다.");
	}

	@Test
	@DisplayName("일반 송금 실패 - 관계 없는 계좌")
	void transferFailNoRelation() {
		Member parent = findParent();
		Account unrelatedAccount = findAccountOfUnrelatedMember(parent.getId());

		TransferRequestDto request = new TransferRequestDto();
		request.setAccountId(unrelatedAccount.getId());
		request.setAmount(new BigDecimal("5000"));
		request.setPassword("123456");

		assertThatThrownBy(() -> transferService.transfer(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("송금 사전 정보 조회 성공 - 적금 계좌")
	void getTransferPrepareInfoSavingsSuccess() {
		Member parent = findParent();
		Account kidSavings = findLinkedKidSavingsAccount(parent.getId());

		TransferPrepareResponseDto result =
			transferService.getTransferPrepareInfo(parent.getId(), kidSavings.getId());

		assertThat(result).isNotNull();
		assertThat(result.getAccountId()).isEqualTo(kidSavings.getId());
		assertThat(result.getTargetMemberName()).isEqualTo(kidSavings.getMember().getName());
		assertThat(result.getAccountAlias()).isEqualTo(kidSavings.getName());
		assertThat(result.getCurrentSaving()).isEqualByComparingTo(kidSavings.getBalance());
		assertThat(result.getSavingLimit()).isEqualByComparingTo(kidSavings.getTotalLimit());
	}

	@Test
	@DisplayName("송금 사전 정보 조회 성공 - 일반 계좌")
	void getTransferPrepareInfoNonSavingsSuccess() {
		Member parent = findParent();
		Account kidAccount = findLinkedKidCheckingAccount(parent.getId());

		TransferPrepareResponseDto result =
			transferService.getTransferPrepareInfo(parent.getId(), kidAccount.getId());

		assertThat(result).isNotNull();
		assertThat(result.getAccountId()).isEqualTo(kidAccount.getId());
		assertThat(result.getCurrentSaving()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.getSavingLimit()).isNotNull();
	}

	@Test
	@DisplayName("송금 사전 정보 조회 실패 - 존재하지 않는 계좌")
	void getTransferPrepareInfoFailNoAccount() {
		Member parent = findParent();

		assertThatThrownBy(() -> transferService.getTransferPrepareInfo(parent.getId(), 999999L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("존재하지 않는 계좌입니다.");
	}

	@Test
	@DisplayName("송금 사전 정보 조회 실패 - 관계 없는 계좌")
	void getTransferPrepareInfoFailNoRelation() {
		Member parent = findParent();
		Account unrelatedAccount = findAccountOfUnrelatedMember(parent.getId());

		assertThatThrownBy(() -> transferService.getTransferPrepareInfo(parent.getId(), unrelatedAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("최근 적금 송금 내역 조회 성공")
	void getRecentTransferAmountSuccess() {
		Member parent = findParent();
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("12000"));
		request.setPassword("123456");
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
		Member parent = findParent();
		Long unlinkedId = findUnlinkedAccountId(parent.getId());

		assertThatThrownBy(() -> transferService.getRecentTransferAmount(parent.getId(), unlinkedId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("적금 릴레이 내역 조회 성공")
	void getRelayHistorySuccessTest() {
		Member parent = findParent();
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("50000"));
		request.setPassword("123456");
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
		Member parent = findParent();

		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), 999L, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");

		Long unlinkedId = findUnlinkedAccountId(parent.getId());
		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), unlinkedId, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");

		Account checkingAccount = findLinkedKidCheckingAccount(parent.getId());
		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), checkingAccount.getId(), 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("적금 계좌만 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("최근 적금 릴레이 내역 조회 성공")
	void getRecentRelayHistorySuccess() {
		Member parent = findParent();
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		SavingsTransferRequestDTO request1 = new SavingsTransferRequestDTO();
		request1.setTargetAccountId(kidSavingsAccount.getId());
		request1.setAmount(new BigDecimal("1000"));
		request1.setPassword("123456");
		request1.setContent("첫 번째");

		SavingsTransferRequestDTO request2 = new SavingsTransferRequestDTO();
		request2.setTargetAccountId(kidSavingsAccount.getId());
		request2.setAmount(new BigDecimal("2000"));
		request2.setPassword("123456");
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
		Member parent = findParent();
		Account checkingAccount = findLinkedKidCheckingAccount(parent.getId());

		assertThatThrownBy(() -> transferService.getRecentRelayHistory(parent.getId(), checkingAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("적금 계좌만 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 성공 - 전체 조회")
	void getExpiredSavingsDetail_Success() {
		Account expiredSavings = accountRepository.findAll().stream()
			.filter(a -> Boolean.TRUE.equals(a.getIsEnd()))
			.filter(a -> a.getAccountType() == AccountType.SAVINGS)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("테스트를 위한 만기된 적금 계좌가 DB에 없습니다."));

		Long ownerId = expiredSavings.getMember().getId();

		SavingsDetailResponseDTO result =
			transferService.getExpiredSavingsDetail(ownerId, expiredSavings.getId(), 0, null);

		assertThat(result).isNotNull();
		assertThat(result.getSenders()).isNotNull();
		assertThat(result.getProductName()).isNotBlank();

		String rawAccountNumber =
			accountCryptoService.decrypt(expiredSavings.getAccountNumber()).replaceAll("-", "");
		String resultAccountNumber =
			result.getAccountNumber().replaceAll("-", "");
		assertThat(resultAccountNumber).isEqualTo(rawAccountNumber);

		assertThat(result.getTransactions()).isNotNull();
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 성공 - 특정 발신인 필터링")
	void getExpiredSavingsDetail_Filter_Success() {
		Account expiredSavings = accountRepository.findAll().stream()
			.filter(a -> Boolean.TRUE.equals(a.getIsEnd()))
			.filter(a -> a.getAccountType() == AccountType.SAVINGS)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("테스트용 만기 적금 계좌가 없습니다."));

		Account senderAcc = accountRepository.findAll().stream()
			.filter(a -> !a.getId().equals(expiredSavings.getId()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 송금 계좌가 없습니다."));

		Transaction tx = Transaction.builder()
			.senderAccount(senderAcc)
			.receiverAccount(expiredSavings)
			.transactionMoney(new BigDecimal("1000"))
			.transactionBalance(new BigDecimal("100000"))
			.transactionType(TransactionType.SAVINGS_DEPOSIT)
			.build();
		transactionRepository.save(tx);

		Letter letter = Letter.builder()
			.content("테스트 편지")
			.transaction(tx)
			.build();
		letterRepository.save(letter);

		Long ownerId = expiredSavings.getMember().getId();

		SavingsDetailResponseDTO unfiltered =
			transferService.getExpiredSavingsDetail(ownerId, expiredSavings.getId(), 0, null);

		assertThat(unfiltered.getSenders()).isNotEmpty();

		Long specificSenderId = unfiltered.getSenders().get(0).getSenderId();

		SavingsDetailResponseDTO result =
			transferService.getExpiredSavingsDetail(ownerId, expiredSavings.getId(), 0, specificSenderId);

		assertThat(result).isNotNull();
		assertThat(result.getTransactions()).isNotEmpty();

		result.getTransactions().forEach(txData ->
			assertThat(txData.getSenderId()).isEqualTo(specificSenderId)
		);
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 권한 없음")
	void getExpiredSavingsDetail_Fail_NotOwner() {
		Account expiredSavings = accountRepository.findAll().stream()
			.filter(a -> Boolean.TRUE.equals(a.getIsEnd()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 만기 계좌가 없습니다."));

		Long strangerId = expiredSavings.getMember().getId() + 100;

		assertThatThrownBy(() -> transferService.getExpiredSavingsDetail(strangerId, expiredSavings.getId(), 0, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("본인의 계좌만 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 만기되지 않은 계좌")
	void getExpiredSavingsDetail_Fail_NotExpired() {
		Account activeAccount = accountRepository.findAll().stream()
			.filter(a -> !Boolean.TRUE.equals(a.getIsEnd()))
			.filter(a -> a.getAccountType() == AccountType.SAVINGS)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 활성 적금 계좌가 없습니다."));

		Long ownerId = activeAccount.getMember().getId();

		assertThatThrownBy(() -> transferService.getExpiredSavingsDetail(ownerId, activeAccount.getId(), 0, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("만기된 계좌만 상세 조회가 가능합니다.");
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 계좌 없음")
	void getExpiredSavingsDetail_Fail_NoAccount() {
		assertThatThrownBy(() -> transferService.getExpiredSavingsDetail(1L, 999999L, 0, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("계좌를 찾을 수 없습니다.");
	}

	private Account findAccountOfUnrelatedMember(Long parentId) {
		return accountRepository.findAll().stream()
			.filter(account -> !account.getMember().getId().equals(parentId))
			.filter(account -> !relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
				parentId,
				account.getMember().getId(),
				MemberRole.KID
			))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 관계 없는 회원의 계좌를 찾을 수 없습니다."));
	}

	private Long findUnlinkedAccountId(Long memberId) {
		return accountRepository.findAll().stream()
			.filter(a -> linkedAccountRepository.findByMemberIdAndAccountId(memberId, a.getId()).isEmpty())
			.map(Account::getId)
			.findFirst().orElseThrow();
	}
}
