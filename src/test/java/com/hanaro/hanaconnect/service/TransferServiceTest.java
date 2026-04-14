package com.hanaro.hanaconnect.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.SavingTransactionRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

	@Mock
	AccountRepository accountRepository;

	@Mock
	SavingTransactionRepository transactionRepository;

	@Mock
	PasswordEncoder passwordEncoder;

	@InjectMocks
	TransferService transferService;

	@Test
	void transferToChildSavings_success() {
		Long memberId = 1L;

		Account sender = createAccount(
			1L,
			AccountType.FREE,
			new BigDecimal("50000"),
			null
		);

		Account receiver = createAccount(
			2L,
			AccountType.SAVINGS,
			new BigDecimal("10000"),
			new BigDecimal("100000")
		);

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO(
			2L,
			new BigDecimal("10000"),
			"1111",
			"적금 응원 편지"
		);

		given(accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE))
			.willReturn(Optional.of(sender));
		given(accountRepository.findByIdWithLock(request.getTargetAccountId()))
			.willReturn(Optional.of(receiver));
		given(passwordEncoder.matches("1111", "encoded-password"))
			.willReturn(true);
		given(transactionRepository.save(any(Transaction.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		SavingsTransferResponseDTO response = transferService.transferToChildSavings(memberId, request);

		assertEquals(0, response.getTransactionMoney().compareTo(new BigDecimal("10000")));
		assertEquals(0, response.getTransactionBalance().compareTo(new BigDecimal("40000")));
		assertEquals("적금 응원 편지", response.getMessage());

		assertEquals(0, sender.getBalance().compareTo(new BigDecimal("40000")));
		assertEquals(0, receiver.getBalance().compareTo(new BigDecimal("20000")));

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		then(transactionRepository).should().save(transactionCaptor.capture());

		Transaction savedTransaction = transactionCaptor.getValue();
		assertEquals(0, savedTransaction.getTransactionMoney().compareTo(new BigDecimal("10000")));
		assertEquals(0, savedTransaction.getTransactionBalance().compareTo(new BigDecimal("40000")));
		assertEquals(TransactionType.SAVINGS_TRANSFER, savedTransaction.getTransactionType());
		assertSame(sender, savedTransaction.getSenderAccount());
		assertSame(receiver, savedTransaction.getReceiverAccount());
	}

	@Test
	void transferToChildSavings_fail_invalid_amount() {
		Long memberId = 1L;

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO(
			2L,
			BigDecimal.ZERO,
			"1111",
			"적금 응원 편지"
		);

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> transferService.transferToChildSavings(memberId, request)
		);

		assertEquals("송금 금액은 0보다 커야 합니다.", exception.getMessage());

		then(accountRepository).shouldHaveNoInteractions();
		then(transactionRepository).shouldHaveNoInteractions();
	}

	@Test
	void transferToChildSavings_fail_wallet_not_found() {
		Long memberId = 1L;

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO(
			2L,
			new BigDecimal("10000"),
			"1111",
			"적금 응원 편지"
		);

		given(accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE))
			.willReturn(Optional.empty());

		EntityNotFoundException exception = assertThrows(
			EntityNotFoundException.class,
			() -> transferService.transferToChildSavings(memberId, request)
		);

		assertEquals("지갑 계좌를 찾을 수 없습니다.", exception.getMessage());

		then(transactionRepository).shouldHaveNoInteractions();
	}

	@Test
	void transferToChildSavings_fail_receiver_not_found() {
		Long memberId = 1L;

		Account sender = createAccount(
			1L,
			AccountType.FREE,
			new BigDecimal("50000"),
			null
		);

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO(
			2L,
			new BigDecimal("10000"),
			"1111",
			"적금 응원 편지"
		);

		given(accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE))
			.willReturn(Optional.of(sender));
		given(accountRepository.findByIdWithLock(request.getTargetAccountId()))
			.willReturn(Optional.empty());

		EntityNotFoundException exception = assertThrows(
			EntityNotFoundException.class,
			() -> transferService.transferToChildSavings(memberId, request)
		);

		assertEquals("대상 적금 계좌를 찾을 수 없습니다.", exception.getMessage());

		then(transactionRepository).shouldHaveNoInteractions();
	}

	@Test
	void transferToChildSavings_fail_wrong_account_password() {
		Long memberId = 1L;

		Account sender = createAccount(
			1L,
			AccountType.FREE,
			new BigDecimal("50000"),
			null
		);

		Account receiver = createAccount(
			2L,
			AccountType.SAVINGS,
			new BigDecimal("10000"),
			new BigDecimal("100000")
		);

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO(
			2L,
			new BigDecimal("10000"),
			"0000",
			"적금 응원 편지"
		);

		given(accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE))
			.willReturn(Optional.of(sender));
		given(accountRepository.findByIdWithLock(request.getTargetAccountId()))
			.willReturn(Optional.of(receiver));
		given(passwordEncoder.matches("0000", "encoded-password"))
			.willReturn(false);

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> transferService.transferToChildSavings(memberId, request)
		);

		assertEquals("계좌 비밀번호가 일치하지 않습니다.", exception.getMessage());

		then(transactionRepository).shouldHaveNoInteractions();
	}

	@Test
	void transferToChildSavings_fail_insufficient_balance() {
		Long memberId = 1L;

		Account sender = createAccount(
			1L,
			AccountType.FREE,
			new BigDecimal("5000"),
			null
		);

		Account receiver = createAccount(
			2L,
			AccountType.SAVINGS,
			new BigDecimal("10000"),
			new BigDecimal("100000")
		);

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO(
			2L,
			new BigDecimal("10000"),
			"1111",
			"적금 응원 편지"
		);

		given(accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE))
			.willReturn(Optional.of(sender));
		given(accountRepository.findByIdWithLock(request.getTargetAccountId()))
			.willReturn(Optional.of(receiver));
		given(passwordEncoder.matches("1111", "encoded-password"))
			.willReturn(true);

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> transferService.transferToChildSavings(memberId, request)
		);

		assertEquals("잔액이 부족합니다.", exception.getMessage());

		then(transactionRepository).shouldHaveNoInteractions();
	}

	@Test
	void transferToChildSavings_fail_total_limit_exceeded() {
		Long memberId = 1L;

		Account sender = createAccount(
			1L,
			AccountType.FREE,
			new BigDecimal("50000"),
			null
		);

		Account receiver = createAccount(
			2L,
			AccountType.SAVINGS,
			new BigDecimal("95000"),
			new BigDecimal("100000")
		);

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO(
			2L,
			new BigDecimal("10000"),
			"1111",
			"적금 응원 편지"
		);

		given(accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE))
			.willReturn(Optional.of(sender));
		given(accountRepository.findByIdWithLock(request.getTargetAccountId()))
			.willReturn(Optional.of(receiver));
		given(passwordEncoder.matches("1111", "encoded-password"))
			.willReturn(true);

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> transferService.transferToChildSavings(memberId, request)
		);

		assertEquals("적금 한도를 초과하셨어요.", exception.getMessage());

		then(transactionRepository).should(never()).save(any(Transaction.class));
	}

	private Account createAccount(
		Long id,
		AccountType accountType,
		BigDecimal balance,
		BigDecimal totalLimit
	) {
		return Account.builder()
			.id(id)
			.name(accountType == AccountType.FREE ? "자유 입출금" : "아이 적금")
			.accountNumber("1000000000" + id)
			.password("encoded-password")
			.accountType(accountType)
			.balance(balance)
			.totalLimit(totalLimit)
			.build();
	}
}
