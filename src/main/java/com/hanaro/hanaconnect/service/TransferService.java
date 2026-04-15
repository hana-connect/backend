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
import com.hanaro.hanaconnect.dto.SavingsDetailResponseDTO;
import com.hanaro.hanaconnect.dto.SavingsTransactionDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.dto.TransferRequestDto;
import com.hanaro.hanaconnect.dto.TransferResponseDto;
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

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final LinkedAccountRepository linkedAccountRepository;
	private final PhoneNameRepository phoneNameRepository;
	private final RelationRepository relationRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final LetterRepository letterRepository;

	// 적금
	@Transactional
	public SavingsTransferResponseDTO transferToChildSavings(Long memberId, SavingsTransferRequestDTO request) {

		// 0. 사용자 조회
		Member parent = memberRepository.findById(memberId)
		.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

		BigDecimal amount = request.getAmount();

		// 1. 계좌 조회
		Account sender = accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE)
			.orElseThrow(() -> new IllegalArgumentException("지갑 계좌를 찾을 수 없습니다."));

		Account receiver = accountRepository.findByIdWithLock(request.getTargetAccountId())
			.orElseThrow(() -> new IllegalArgumentException("대상 적금 계좌를 찾을 수 없습니다."));

		// 2. 연결된 계좌인지 확인
		boolean isLinked = linkedAccountRepository.existsByAccountIdAndMemberId(request.getTargetAccountId(), memberId);
		if (!isLinked) {
			throw new IllegalArgumentException("연결된 손주 계좌가 아닙니다.");
		}

		// 3. 비밀번호 확인
		validatePassword(request.getPassword(), parent.getPassword());

		// 4. 적금 계좌 한도 확인
		if (receiver.getTotalLimit() != null) {
			BigDecimal newBalance = receiver.getBalance().add(amount);
			if (newBalance.compareTo(receiver.getTotalLimit()) > 0) {
				throw new IllegalArgumentException("적금 한도를 초과했어요.");
			}
		}

		// 5. 출금
		sender.withdraw(amount);

		// 6. 입금
		receiver.deposit(amount);

		// 7. 거래 저장
		// (1) 할머니 지갑 기준 출금 내역 저장
		Transaction withdrawTx = createTransaction(sender, receiver, amount, sender.getBalance(), TransactionType.SAVINGS_WITHDRAW);
		transactionRepository.save(withdrawTx);

		// (2) 아이 적금 계좌 기준 입금 내역 저장
		Transaction depositTx = createTransaction(sender, receiver, amount, receiver.getBalance(), TransactionType.SAVINGS_DEPOSIT);
		Transaction savedTransaction = transactionRepository.save(depositTx);

		// 메시지 정규화
		String normalizedContent = (request.getContent() == null) ? null : request.getContent().trim();

		// 정규화된 값이 진짜 내용이 있을 때만 Letter 저장
		if (normalizedContent != null && !normalizedContent.isEmpty()) {
			Letter letter = Letter.builder()
				.content(normalizedContent)
				.transaction(savedTransaction)
				.build();
			letterRepository.save(letter);
		}

		// 8. 응답
		return SavingsTransferResponseDTO.builder()
			.transactionMoney(savedTransaction.getTransactionMoney())
			.transactionBalance(savedTransaction.getTransactionBalance())
			.message(normalizedContent)
			.build();
	}

	// 송금
	@Transactional
	public TransferResponseDto transfer(Long loginMemberId, TransferRequestDto request) {

		Member parent = memberRepository.findById(loginMemberId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

		Account parentAccount = accountRepository
			.findByMemberIdAndAccountTypeWithLock(loginMemberId, AccountType.FREE)
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
		transactionRepository.save(depositTransaction);

		return TransferResponseDto.builder()
			.accountNumber(kidAccount.getAccountNumber())
			.amount(amount)
			.transferDate(LocalDate.now())
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

		return TransferPrepareResponseDto.builder()
			.accountId(accountId)
			.targetMemberName(kid.getName())
			.phoneSavedName(phoneSavedName)
			.displayName(displayName)
			.accountAlias(kidAccount.getName())
			.balance(parentAccount.getBalance())
			.build();
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

	@Transactional(readOnly = true)
	public RelayResponseDTO getRelayHistory(Long memberId, Long targetAccountId) {
		LinkedAccount linkedAccount = linkedAccountRepository.findByMemberIdAndAccountId(memberId, targetAccountId)
			.orElseThrow(() -> new IllegalArgumentException("해당 계좌에 접근 권한이 없습니다."));

		Account account = linkedAccount.getAccount();

		if (account.getAccountType() != AccountType.SAVINGS) {
			throw new IllegalArgumentException("적금 계좌만 조회할 수 있습니다.");
		}

		String nickname = linkedAccount.getNickname();
		String displayName = (nickname != null && !nickname.isBlank()) ? nickname : account.getName();

		List<RelayHistoryDTO> history = letterRepository.findMyRelayHistory(memberId, targetAccountId);

		return RelayResponseDTO.builder()
			.productNickname(displayName)
			.accountNumber(account.getAccountNumber())
			.history(history)
			.build();
	}

	@Transactional(readOnly = true)
	public SavingsDetailResponseDTO getExpiredSavingsDetail(Long memberId, Long accountId) {
		Account account = accountRepository.findById(accountId)
			.orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));

		if (!account.getMember().getId().equals(memberId)) {
			throw new IllegalArgumentException("본인의 계좌만 조회할 수 있습니다.");
		}

		// 만기 여부 및 타입 확인
		if (!Boolean.TRUE.equals(account.getIsEnd())) {
			throw new IllegalArgumentException("만기된 계좌만 상세 조회가 가능합니다.");
		}

		if (account.getAccountType() != AccountType.SAVINGS) {
			throw new IllegalArgumentException("적금 계좌가 아닙니다.");
		}

		// 거래 내역 조회
		List<SavingsTransactionDTO> transactions = letterRepository.findAllSavingsDetails(accountId);

		return SavingsDetailResponseDTO.builder()
			.productName(account.getName())
			.accountNumber(account.getAccountNumber())
			.transactions(transactions)
			.build();
	}
}
