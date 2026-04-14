package com.hanaro.hanaconnect.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.SavingTransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferService {

	private final AccountRepository accountRepository;
	private final SavingTransactionRepository transactionRepository;
	private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	@Transactional
	public SavingsTransferResponseDTO transferToChildSavings(Long memberId, SavingsTransferRequestDTO request) {

		// 금액이 0원 이하이거나 없는지 가장 먼저 확인!
		if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("송금 금액은 0보다 커야 합니다.");
		}

		// 1. 계좌들 조회
		Account sender = accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE)
			.orElseThrow(() -> new EntityNotFoundException("지갑 계좌를 찾을 수 없습니다."));

		Account receiver = accountRepository.findByIdWithLock(request.getTargetAccountId())
			.orElseThrow(() -> new EntityNotFoundException("대상 적금 계좌를 찾을 수 없습니다."));

		// 2. 검증: 비밀번호 확인
		if (!passwordEncoder.matches(request.getAccountPassword(), sender.getPassword())) {
			throw new IllegalArgumentException("계좌 비밀번호가 일치하지 않습니다.");
		}

		// 3. 검증: [잔액] 체크 (보내는 사람 지갑 통장 기준)
		sender.withdraw(request.getAmount());

		// 4. 검증: [적금 계좌 한도] 체크 (아이 적금 통장 기준)
		if (receiver.getTotalLimit() != null) {
			BigDecimal currentBalance = receiver.getBalance();
			BigDecimal newBalance = currentBalance.add(request.getAmount());

			if (newBalance.compareTo(receiver.getTotalLimit()) > 0) {
				throw new IllegalArgumentException("적금 한도를 초과하셨어요.");
			}
		}

		// 5. 이체 실행 (상태 변경)
		receiver.deposit(request.getAmount());

		// 6. Transaction 저장 (기록)
		Transaction transaction = Transaction.builder()
			.transactionMoney(request.getAmount())
			.transactionBalance(sender.getBalance())
			.transactionType(TransactionType.SAVINGS_TRANSFER)
			.senderAccount(sender)
			.receiverAccount(receiver)
			.build();

		Transaction savedTransaction = transactionRepository.save(transaction);

		// 7. 결과 반환
		return SavingsTransferResponseDTO.builder()
			.transactionMoney(savedTransaction.getTransactionMoney())
			.transactionBalance(savedTransaction.getTransactionBalance())
			.message(request.getContent())
			.build();
	}
}
