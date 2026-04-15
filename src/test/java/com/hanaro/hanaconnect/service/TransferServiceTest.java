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
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.TransferRequestDto;
import com.hanaro.hanaconnect.dto.TransferResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;

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
	@DisplayName("적금 릴레이 내역 조회 성공")
	void getRelayHistorySuccessTest() {
		// Given
		Member parent = findParent();
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		// 내역 만들기
		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("50000"));
		request.setPassword("123456");
		request.setContent("할머니가 주는 용돈이다!");

		transferService.transferToChildSavings(parent.getId(), request);

		// When
		com.hanaro.hanaconnect.dto.RelayResponseDTO result =
			transferService.getRelayHistory(parent.getId(), kidSavingsAccount.getId());

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getProductNickname()).isEqualTo(kidSavingsAccount.getName());

		// history 검증
		assertThat(result.getHistory()).isNotEmpty();
		assertThat(result.getHistory().get(0).getMessage()).isEqualTo("할머니가 주는 용돈이다!");
		assertThat(result.getHistory().get(0).getAmount()).isEqualByComparingTo("50000");
	}

	@Test
	@DisplayName("적금 릴레이 내역 조회 실패 케이스 모음")
	void getRelayHistoryFailTest() {
		Member parent = findParent();

		// 존재하지 않는 계좌
		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), 999L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");

		// 내 연결 계좌 목록에 없는 진짜 계좌 ID
		Long unlinkedId = findUnlinkedAccountId(parent.getId());
		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), unlinkedId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 계좌에 접근 권한이 없습니다.");

		// 계좌 타입 불일치
		Account checkingAccount = findLinkedKidCheckingAccount(parent.getId());
		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), checkingAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("적금 계좌만 조회할 수 있습니다.");
	}

	private Account findLinkedKidCheckingAccount(Long memberId) {
		return linkedAccountRepository.findAllByMemberId(memberId).stream()
			.map(LinkedAccount::getAccount)
			.filter(a -> a.getAccountType() == AccountType.FREE)
			.findFirst().orElseThrow();
	}

	private Long findUnlinkedAccountId(Long memberId) {
		return accountRepository.findAll().stream()
			.filter(a -> linkedAccountRepository.findByMemberIdAndAccountId(memberId, a.getId()).isEmpty())
			.map(Account::getId)
			.findFirst().orElseThrow();
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 성공")
	void getExpiredSavingsDetail_Success() {
		Account expiredSavings = accountRepository.findAll().stream()
			.filter(a -> Boolean.TRUE.equals(a.getIsEnd()))
			.filter(a -> a.getAccountType() == AccountType.SAVINGS)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("테스트를 위한 만기된 적금 계좌가 DB에 없습니다."));

		Long ownerId = expiredSavings.getMember().getId();

		com.hanaro.hanaconnect.dto.SavingsDetailResponseDTO result =
			transferService.getExpiredSavingsDetail(ownerId, expiredSavings.getId());

		assertThat(result).isNotNull();
		assertThat(result.getProductName()).isEqualTo(expiredSavings.getName());
		assertThat(result.getAccountNumber()).isEqualTo(expiredSavings.getAccountNumber());
		assertThat(result.getTransactions()).isNotNull();
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 권한 없음")
	void getExpiredSavingsDetail_Fail_NotOwner() {
		Account expiredSavings = accountRepository.findAll().stream()
			.filter(a -> Boolean.TRUE.equals(a.getIsEnd()))
			.findFirst()
			.orElseThrow();

		Long strangerId = expiredSavings.getMember().getId() + 100;

		assertThatThrownBy(() -> transferService.getExpiredSavingsDetail(strangerId, expiredSavings.getId()))
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
			.orElseThrow();

		Long ownerId = activeAccount.getMember().getId();

		assertThatThrownBy(() -> transferService.getExpiredSavingsDetail(ownerId, activeAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("만기된 계좌만 상세 조회가 가능합니다.");
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 적금 타입이 아님")
	void getExpiredSavingsDetail_Fail_NotSavings() {
		Account freeAccount = accountRepository.findAll().stream()
			.filter(a -> a.getAccountType() == AccountType.FREE)
			.findFirst()
			.orElseThrow();

		Long ownerId = freeAccount.getMember().getId();

		assertThatThrownBy(() -> transferService.getExpiredSavingsDetail(ownerId, freeAccount.getId()))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("일반 송금 성공")
	void transferSuccessTest() {
		Member parent = findParent();
		Account parentFreeAccount = findParentFreeAccount(parent.getId());
		Account kidFreeAccount = findLinkedKidFreeAccount(parent.getId());

		BigDecimal senderBefore = parentFreeAccount.getBalance();
		BigDecimal receiverBefore = kidFreeAccount.getBalance();

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(kidFreeAccount.getId())
			.amount(new BigDecimal("10000"))
			.password("123456")
			.build();

		TransferResponseDto result = transferService.transfer(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getTransferId()).isNotNull();
		assertThat(result.getToAccountId()).isEqualTo(kidFreeAccount.getId());
		assertThat(result.getToAccountNumber()).isEqualTo(kidFreeAccount.getAccountNumber());
		assertThat(result.getAmount()).isEqualByComparingTo("10000");
		assertThat(result.getTransferredAt()).isNotNull();

		Account updatedSender = accountRepository.findById(parentFreeAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("부모 계좌를 다시 찾을 수 없습니다."));
		Account updatedReceiver = accountRepository.findById(kidFreeAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("아이 계좌를 다시 찾을 수 없습니다."));

		assertThat(updatedSender.getBalance())
			.isEqualByComparingTo(senderBefore.subtract(new BigDecimal("10000")));
		assertThat(updatedReceiver.getBalance())
			.isEqualByComparingTo(receiverBefore.add(new BigDecimal("10000")));
	}

	@Test
	@DisplayName("일반 송금 실패 - 비밀번호 불일치")
	void transferFailWrongPasswordTest() {
		Member parent = findParent();
		Account kidFreeAccount = findLinkedKidFreeAccount(parent.getId());

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(kidFreeAccount.getId())
			.amount(new BigDecimal("10000"))
			.password("000000")
			.build();

		assertThatThrownBy(() -> transferService.transfer(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("비밀번호가 일치하지 않습니다.");
	}

	@Test
	@DisplayName("송금 결과 조회 성공")
	void getTransferResultSuccessTest() {
		Member parent = findParent();
		Account kidFreeAccount = findLinkedKidFreeAccount(parent.getId());

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(kidFreeAccount.getId())
			.amount(new BigDecimal("15000"))
			.password("123456")
			.build();

		TransferResponseDto transferResult =
			transferService.transfer(parent.getId(), request);

		TransferResponseDto result =
			transferService.getTransferResult(parent.getId(), transferResult.getTransferId());

		assertThat(result).isNotNull();
		assertThat(result.getTransferId()).isEqualTo(transferResult.getTransferId());
		assertThat(result.getToAccountId()).isEqualTo(kidFreeAccount.getId());
		assertThat(result.getToAccountNumber()).isEqualTo(kidFreeAccount.getAccountNumber());
		assertThat(result.getAmount()).isEqualByComparingTo("15000");
		assertThat(result.getTransferredAt()).isNotNull();
	}

	@Test
	@DisplayName("송금 결과 조회 실패 - 존재하지 않는 거래")
	void getTransferResultFailNotFoundTest() {
		Member parent = findParent();

		assertThatThrownBy(() -> transferService.getTransferResult(parent.getId(), 999999L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("거래를 찾을 수 없습니다.");
	}

	private Account findLinkedKidFreeAccount(Long parentId) {
		return accountRepository.findAll().stream()
			.filter(account -> account.getAccountType() == AccountType.FREE)
			.filter(account -> linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), parentId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 연결된 아이 입출금 계좌를 찾을 수 없습니다."));
	}

}
