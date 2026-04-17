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
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.saving.RelayHistoryDTO;
import com.hanaro.hanaconnect.dto.saving.RelayResponseDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsDetailResponseDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransactionDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.transfer.RecentTransferResponseDTO;
import com.hanaro.hanaconnect.dto.transfer.SenderInfoDTO;
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
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferService {

	private static final BigDecimal NO_LIMIT = new BigDecimal("1000000000000");

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final LinkedAccountRepository linkedAccountRepository;
	private final PhoneNameRepository phoneNameRepository;
	private final RelationRepository relationRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final LetterRepository letterRepository;
	private final AccountCryptoService accountCryptoService;

	// 적금
	@Transactional
	public SavingsTransferResponseDTO transferToChildSavings(Long memberId, SavingsTransferRequestDTO request) {

		Member parent = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

		BigDecimal amount = request.getAmount();

		Account sender = accountRepository.findByMemberIdAndAccountTypeAndIsRewardFalseWithLock(memberId, AccountType.FREE)
			.orElseThrow(() -> new IllegalArgumentException("지갑 계좌를 찾을 수 없습니다."));

		Account receiver = accountRepository.findByIdWithLock(request.getTargetAccountId())
			.orElseThrow(() -> new IllegalArgumentException("대상 적금 계좌를 찾을 수 없습니다."));

		boolean isLinked = linkedAccountRepository.existsByAccountIdAndMemberId(request.getTargetAccountId(), memberId);
		if (!isLinked) {
			throw new IllegalArgumentException("연결된 손주 계좌가 아닙니다.");
		}

		validatePassword(request.getPassword(), parent.getPassword());

		if (receiver.getTotalLimit() != null) {
			BigDecimal newBalance = receiver.getBalance().add(amount);
			if (newBalance.compareTo(receiver.getTotalLimit()) > 0) {
				throw new IllegalArgumentException("적금 한도를 초과했어요.");
			}
		}

		sender.withdraw(amount);
		receiver.deposit(amount);

		Transaction withdrawTx =
			createTransaction(sender, receiver, amount, sender.getBalance(), TransactionType.SAVINGS_WITHDRAW);
		transactionRepository.save(withdrawTx);

		Transaction depositTx =
			createTransaction(sender, receiver, amount, receiver.getBalance(), TransactionType.SAVINGS_DEPOSIT);
		Transaction savedTransaction = transactionRepository.save(depositTx);

		String normalizedContent = (request.getContent() == null) ? null : request.getContent().trim();

		if (normalizedContent != null && !normalizedContent.isEmpty()) {
			Letter letter = Letter.builder()
				.content(normalizedContent)
				.transaction(savedTransaction)
				.build();
			letterRepository.save(letter);
		}

		return SavingsTransferResponseDTO.builder()
			.transactionMoney(savedTransaction.getTransactionMoney())
			.transactionBalance(savedTransaction.getTransactionBalance())
			.message(normalizedContent)
			.toAccountNumber(accountCryptoService.decrypt(receiver.getAccountNumber()))
			.build();
	}

	// 송금
	@Transactional
	public TransferResponseDto transfer(Long loginMemberId, TransferRequestDto request) {

		Member parent = memberRepository.findById(loginMemberId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

		Account parentAccount = accountRepository
			.findByMemberIdAndAccountTypeAndIsRewardFalseWithLock(loginMemberId, AccountType.FREE)
			.orElseThrow(() -> new IllegalArgumentException("출금 계좌가 없습니다."));

		Account kidAccount = accountRepository.findByIdWithLock(request.getAccountId())
			.orElseThrow(() -> new IllegalArgumentException("대상 계좌가 존재하지 않습니다."));

		Member kid = kidAccount.getMember();

		validateKidRelation(loginMemberId, kid.getId());
		validatePassword(request.getPassword(), parent.getPassword());

		BigDecimal amount = request.getAmount();

		parentAccount.withdraw(amount);
		kidAccount.deposit(amount);

		Transaction withdrawTransaction =
			createTransaction(parentAccount, kidAccount, amount, parentAccount.getBalance(), TransactionType.WITHDRAW);

		Transaction depositTransaction =
			createTransaction(parentAccount, kidAccount, amount, kidAccount.getBalance(), TransactionType.DEPOSIT);

		transactionRepository.save(withdrawTransaction);
		Transaction savedDepositTransaction = transactionRepository.save(depositTransaction);

		return TransferResponseDto.builder()
			.transferId(savedDepositTransaction.getId())
			.toAccountId(kidAccount.getId())
			.toAccountNumber(accountCryptoService.decrypt(kidAccount.getAccountNumber()))
			.amount(amount)
			.transferredAt(savedDepositTransaction.getCreatedAt())
			.build();
	}

	// 송금 직전
	@Transactional(readOnly = true)
	public TransferPrepareResponseDto getTransferPrepareInfo(Long loginMemberId, Long accountId) {

		Account kidAccount = accountRepository.findById(accountId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다."));

		Member kid = kidAccount.getMember();
		Long kidId = kid.getId();

		validateKidRelation(loginMemberId, kidId);

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

		var builder = TransferPrepareResponseDto.builder()
			.accountId(accountId)
			.targetMemberName(kid.getName())
			.phoneSavedName(phoneSavedName)
			.displayName(displayName)
			.accountAlias(kidAccount.getName())
			.balance(parentAccount.getBalance());

		if (kidAccount.getAccountType() == AccountType.SAVINGS) {
			builder.currentSaving(kidAccount.getBalance())
				.savingLimit(kidAccount.getTotalLimit());
		} else {
			builder.currentSaving(BigDecimal.ZERO)
				.savingLimit(NO_LIMIT);
		}

		return builder.build();
	}

	private void validatePassword(String rawPassword, String encodedPassword) {
		if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}
	}

	private void validateKidRelation(Long parentId, Long kidId) {
		boolean isRelated = relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
			parentId,
			kidId,
			MemberRole.KID
		);

		if (!isRelated) {
			throw new IllegalArgumentException("해당 계좌에 접근 권한이 없습니다.");
		}
	}

	private Transaction createTransaction(
		Account sender,
		Account receiver,
		BigDecimal amount,
		BigDecimal balance,
		TransactionType type
	) {
		return Transaction.builder()
			.senderAccount(sender)
			.receiverAccount(receiver)
			.transactionMoney(amount)
			.transactionBalance(balance)
			.transactionType(type)
			.build();
	}

	// 최근 적금 송금 내역 단건 조회
	@Transactional(readOnly = true)
	public RecentTransferResponseDTO getRecentTransferAmount(Long memberId, Long receiverAccountId) {

		validateAndGetSavingsAccount(memberId, receiverAccountId);

		Transaction latestTx = transactionRepository.findTopByReceiverAccountIdAndTransactionTypeOrderByCreatedAtDesc(
			receiverAccountId,
			TransactionType.SAVINGS_WITHDRAW
		).orElse(null);

		if (latestTx == null) {
			return null;
		}

		return RecentTransferResponseDTO.builder()
			.transactionDate(latestTx.getCreatedAt())
			.amount(latestTx.getTransactionMoney())
			.build();
	}

	private LinkedAccount validateAndGetSavingsAccount(Long memberId, Long targetAccountId) {
		LinkedAccount linkedAccount = linkedAccountRepository.findByMemberIdAndAccountId(memberId, targetAccountId)
			.orElseThrow(() -> new IllegalArgumentException("해당 계좌에 접근 권한이 없습니다."));

		if (linkedAccount.getAccount().getAccountType() != AccountType.SAVINGS) {
			throw new IllegalArgumentException("적금 계좌만 조회할 수 있습니다.");
		}

		return linkedAccount;
	}

	// 부모용
	@Transactional(readOnly = true)
	public RelayResponseDTO getRelayHistory(Long memberId, Long targetAccountId, int page) {
		LinkedAccount linkedAccount = validateAndGetSavingsAccount(memberId, targetAccountId);

		Account account = linkedAccount.getAccount();

		org.springframework.data.domain.Pageable pageable =
			org.springframework.data.domain.PageRequest.of(page, 12);

		org.springframework.data.domain.Page<RelayHistoryDTO> historyPage =
			letterRepository.findMyRelayHistory(memberId, targetAccountId, pageable);

		String nickname = linkedAccount.getNickname();
		String displayName = (nickname != null && !nickname.isBlank()) ? nickname : account.getName();

		return RelayResponseDTO.builder()
			.productNickname(displayName)
			.accountNumber(accountCryptoService.decrypt(account.getAccountNumber()))
			.history(historyPage.getContent())
			.isLast(historyPage.isLast())
			.build();
	}

	@Transactional(readOnly = true)
	public RelayResponseDTO getRecentRelayHistory(Long memberId, Long targetAccountId) {
		LinkedAccount linkedAccount = validateAndGetSavingsAccount(memberId, targetAccountId);

		Account account = linkedAccount.getAccount();
		String displayName = (linkedAccount.getNickname() != null && !linkedAccount.getNickname().isBlank())
			? linkedAccount.getNickname() : account.getName();

		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 3);
		List<RelayHistoryDTO> history = letterRepository.findTop3RelayHistory(memberId, targetAccountId, pageable);

		return RelayResponseDTO.builder()
			.productNickname(displayName)
			.accountNumber(accountCryptoService.decrypt(account.getAccountNumber()))
			.history(history)
			.build();
	}

	// 아이용
	@Transactional(readOnly = true)
	public SavingsDetailResponseDTO getExpiredSavingsDetail(
		Long memberId,
		Long accountId,
		int page,
		Long senderId
	) {
		Account account = accountRepository.findById(accountId)
			.orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));

		if (!account.getMember().getId().equals(memberId)) {
			throw new IllegalArgumentException("본인의 계좌만 조회할 수 있습니다.");
		}

		if (!Boolean.TRUE.equals(account.getIsEnd())) {
			throw new IllegalArgumentException("만기된 계좌만 상세 조회가 가능합니다.");
		}

		if (account.getAccountType() != AccountType.SAVINGS) {
			throw new IllegalArgumentException("적금 계좌가 아닙니다.");
		}

		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 12);
		org.springframework.data.domain.Page<SavingsTransactionDTO> transactionsPage =
			letterRepository.findAllSavingsDetails(accountId, senderId, pageable);

		List<SenderInfoDTO> senders = letterRepository.findDistinctSendersByAccountId(accountId);

		return SavingsDetailResponseDTO.builder()
			.productName(account.getName())
			.accountNumber(accountCryptoService.decrypt(account.getAccountNumber()))
			.senders(senders)
			.transactions(transactionsPage.getContent())
			.isLast(transactionsPage.isLast())
			.build();
	}

	@Transactional(readOnly = true)
	public TransferResponseDto getTransferResult(Long loginMemberId, Long transferId) {

		Transaction tx = transactionRepository.findById(transferId)
			.orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

		if (!tx.getSenderAccount().getMember().getId().equals(loginMemberId)) {
			throw new IllegalArgumentException("해당 거래에 접근 권한이 없습니다.");
		}

		if (tx.getTransactionType() != TransactionType.DEPOSIT) {
			throw new IllegalArgumentException("송금 결과 조회 대상이 아닙니다.");
		}

		return TransferResponseDto.builder()
			.transferId(tx.getId())
			.toAccountId(tx.getReceiverAccount().getId())
			.toAccountNumber(accountCryptoService.decrypt(tx.getReceiverAccount().getAccountNumber()))
			.amount(tx.getTransactionMoney())
			.transferredAt(tx.getCreatedAt())
			.build();
	}
}
