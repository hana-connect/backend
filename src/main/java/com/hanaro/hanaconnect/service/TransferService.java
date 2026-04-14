package com.hanaro.hanaconnect.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.dto.RelayHistoryDTO;
import com.hanaro.hanaconnect.dto.RelayResponseDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.dto.TransferRequestDto;
import com.hanaro.hanaconnect.dto.TransferResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Letter;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LetterRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.SavingTransactionRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferService {

	private final AccountRepository accountRepository;

	private final SavingTransactionRepository savingTransactionRepository;
	private final TransactionRepository transactionRepository;

	private final LinkedAccountRepository linkedAccountRepository;
	private final PhoneNameRepository phoneNameRepository;
	private final RelationRepository relationRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final LetterRepository letterRepository;

	@Transactional
	public SavingsTransferResponseDTO transferToChildSavings(Long memberId, SavingsTransferRequestDTO request) {
		// 0. 필수값 검증
		if (request.getAmount() == null
			|| request.getTargetAccountId() == null
			|| request.getAccountPassword() == null) {
			throw new IllegalArgumentException("필수 입력값이 누락되었습니다.");
		}

		if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("송금 금액은 0보다 커야 합니다.");
		}

		// 1. 계좌 조회
		Account sender = accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE)
			.orElseThrow(() -> new EntityNotFoundException("지갑 계좌를 찾을 수 없습니다."));

		Account receiver = accountRepository.findByIdWithLock(request.getTargetAccountId())
			.orElseThrow(() -> new EntityNotFoundException("대상 적금 계좌를 찾을 수 없습니다."));

		// 2. 연결된 계좌인지 확인
		boolean isLinked = linkedAccountRepository.existsByAccountIdAndMemberId(request.getTargetAccountId(), memberId);
		if (!isLinked) {
			throw new IllegalArgumentException("연결된 손주 계좌가 아닙니다.");
		}

		// 3. 비밀번호 확인
		if (!passwordEncoder.matches(request.getAccountPassword(), sender.getPassword())) {
			throw new IllegalArgumentException("계좌 비밀번호가 일치하지 않습니다.");
		}

		// 4. 출금
		sender.withdraw(request.getAmount());

		// 5. 적금 계좌 한도 확인
		if (receiver.getTotalLimit() != null) {
			BigDecimal newBalance = receiver.getBalance().add(request.getAmount());
			if (newBalance.compareTo(receiver.getTotalLimit()) > 0) {
				throw new IllegalArgumentException("적금 한도를 초과했어요.");
			}
		}

		// 6. 입금
		receiver.deposit(request.getAmount());

		// 7. 거래 저장
		Transaction transaction = Transaction.builder()
			.transactionMoney(request.getAmount())
			.transactionBalance(sender.getBalance())
			.transactionType(TransactionType.SAVINGS_TRANSFER)
			.senderAccount(sender)
			.receiverAccount(receiver)
			.build();

		Transaction savedTransaction = savingTransactionRepository.save(transaction);

		if (request.getContent() != null && !request.getContent().isBlank()) {
			Letter letter = Letter.builder()
				.content(request.getContent())
				.transaction(savedTransaction)
				.build();
			letterRepository.save(letter);
		}

		// 8. 응답
		return SavingsTransferResponseDTO.builder()
			.transactionMoney(savedTransaction.getTransactionMoney())
			.transactionBalance(savedTransaction.getTransactionBalance())
			.message(request.getContent())
			.build();
	}

	@Transactional(readOnly = true)
	public TransferPrepareResponseDto getTransferPrepareInfo(Long loginMemberId, Long accountId) {

		Account kidAccount = accountRepository.findById(accountId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다."));

		Member kid = kidAccount.getMember();
		Long kidId = kid.getId();

		boolean isRelated = relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
			loginMemberId,
			kidId,
			MemberRole.KID
		);

		if (!isRelated) {
			throw new IllegalArgumentException("해당 계좌에 접근 권한이 없습니다.");
		}

		String phoneSavedName = phoneNameRepository
			.findNameByOwnerIdAndTargetId(loginMemberId, kidId)
			.orElse(null);

		boolean hasPhoneSavedName = phoneSavedName != null && !phoneSavedName.isBlank();

		String displayName = hasPhoneSavedName
			? kid.getName() + "(" + phoneSavedName + ")"
			: kid.getName();

		Account parentAccount = accountRepository
			.findByMemberIdAndAccountType(loginMemberId, AccountType.FREE)
			.orElseThrow(() -> new IllegalArgumentException("출금 계좌가 없습니다."));

		BigDecimal balance = parentAccount.getBalance();

		return TransferPrepareResponseDto.builder()
			.accountId(accountId)
			.targetMemberName(kid.getName())
			.phoneSavedName(phoneSavedName)
			.displayName(displayName)
			.accountAlias(kidAccount.getName())
			.balance(balance)
			.build();
	}

	@Transactional
	public TransferResponseDto transfer(Long loginMemberId, TransferRequestDto request) {

		Member parent = memberRepository.findById(loginMemberId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

		Account parentAccount = accountRepository
			.findByMemberIdAndAccountType(loginMemberId, AccountType.FREE)
			.orElseThrow(() -> new IllegalArgumentException("출금 계좌가 없습니다."));

		Account kidAccount = accountRepository.findById(request.getAccountId())
			.orElseThrow(() -> new IllegalArgumentException("대상 계좌가 존재하지 않습니다."));

		Member kid = kidAccount.getMember();

		boolean isRelated = relationRepository
			.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
				loginMemberId,
				kid.getId(),
				MemberRole.KID
			);

		if (!isRelated) {
			throw new IllegalArgumentException("해당 계좌에 접근 권한이 없습니다.");
		}

		if (!passwordEncoder.matches(request.getPassword(), parent.getPassword())) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}

		BigDecimal amount = request.getAmount();

		parentAccount.withdraw(amount);
		kidAccount.deposit(amount);

		Transaction withdrawTransaction = Transaction.builder()
			.senderAccount(parentAccount)
			.receiverAccount(kidAccount)
			.transactionMoney(amount)
			.transactionBalance(parentAccount.getBalance())
			.transactionType(TransactionType.WITHDRAW)
			.build();

		Transaction depositTransaction = Transaction.builder()
			.senderAccount(parentAccount)
			.receiverAccount(kidAccount)
			.transactionMoney(amount)
			.transactionBalance(kidAccount.getBalance())
			.transactionType(TransactionType.DEPOSIT)
			.build();

		transactionRepository.save(withdrawTransaction);
		transactionRepository.save(depositTransaction);

		return TransferResponseDto.builder()
			.accountNumber(kidAccount.getAccountNumber())
			.amount(amount)
			.transferDate(LocalDate.now())
			.build();
	}

	@Transactional(readOnly = true)
	public RelayResponseDTO getRelayHistory(Long memberId, Long targetAccountId) {
		// 계좌 정보 조회
		Account account = accountRepository.findById(targetAccountId)
			.orElseThrow(() -> new RuntimeException("계좌를 찾을 수 없습니다."));

		// Repository에서 만든 쿼리로 편지 내역 리스트 조회
		List<RelayHistoryDTO> history = letterRepository.findMyRelayHistory(memberId, targetAccountId);

		return RelayResponseDTO.builder()
			.productName(account.getName())
			.accountNumber(account.getAccountNumber())
			.history(history)
			.build();
	}
}
