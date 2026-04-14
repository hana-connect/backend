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
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.SavingTransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;

import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferService {

	private final AccountRepository accountRepository;
	private final SavingTransactionRepository transactionRepository;
	private final LinkedAccountRepository linkedAccountRepository;
	private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	@Transactional
	public SavingsTransferResponseDTO transferToChildSavings(Long memberId, SavingsTransferRequestDTO request) {
		// 0. 필수값 검증 (맨 위로 이동)
		if (request.getAmount() == null
			|| request.getTargetAccountId() == null
			|| request.getAccountPassword() == null
		) {
			throw new IllegalArgumentException("필수 입력값이 누락되었습니다.");
		}

		// 금액이 0원 이하이거나 없는지 가장 먼저 확인!
		if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0)  {
			throw new IllegalArgumentException("송금 금액은 0보다 커야 합니다.");
		}

		// 1. 계좌들 조회
		Account sender = accountRepository.findByMemberIdAndAccountTypeWithLock(memberId, AccountType.FREE)
			.orElseThrow(() -> new EntityNotFoundException("지갑 계좌를 찾을 수 없습니다."));

		Account receiver = accountRepository.findByIdWithLock(request.getTargetAccountId())
			.orElseThrow(() -> new EntityNotFoundException("대상 적금 계좌를 찾을 수 없습니다."));

		// 2. 검증: 연결된 손주 계좌인지 확인
		boolean isLinked = linkedAccountRepository.existsByAccountIdAndMemberId(request.getTargetAccountId(), memberId);
		if (!isLinked) {
			throw new IllegalArgumentException("연결된 손주 계좌가 아닙니다.");
		}

		// 3. 검증: 비밀번호 확인
		if (!passwordEncoder.matches(request.getAccountPassword(), sender.getPassword())) {
			throw new IllegalArgumentException("계좌 비밀번호가 일치하지 않습니다.");
		}

		// 4. 검증: [잔액] 체크 (보내는 사람 지갑 통장 기준)
		sender.withdraw(request.getAmount());

		// 5. 검증: [적금 계좌 한도] 체크 (아이 적금 통장 기준)
		if (receiver.getTotalLimit() != null) {
			BigDecimal currentBalance = receiver.getBalance();
			BigDecimal newBalance = currentBalance.add(request.getAmount());

			if (newBalance.compareTo(receiver.getTotalLimit()) > 0) {
				throw new IllegalArgumentException("적금 한도를 초과했어요.");
			}
		}

		// 6. 이체 실행 (상태 변경)
		receiver.deposit(request.getAmount());

		// 7. Transaction 저장 (기록)
		Transaction transaction = Transaction.builder()
			.transactionMoney(request.getAmount())
			.transactionBalance(sender.getBalance())
			.transactionType(TransactionType.SAVINGS_TRANSFER)
			.senderAccount(sender)
			.receiverAccount(receiver)
			.build();

		Transaction savedTransaction = transactionRepository.save(transaction);

		// 8. 결과 반환
		return SavingsTransferResponseDTO.builder()
			.transactionMoney(savedTransaction.getTransactionMoney())
			.transactionBalance(savedTransaction.getTransactionBalance())
			.message(request.getContent())
    }

	@Transactional(readOnly = true)
	public TransferPrepareResponseDto getTransferPrepareInfo(Long loginMemberId, Long accountId) {

		// 1. 아이 계좌 조회
		Account kidAccount = accountRepository.findById(accountId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다."));

		// 2. 아이 회원 조회
		Member kid = kidAccount.getMember();
		Long kidId = kid.getId();

		// 3. 부모-자식 관계 검증
		boolean isRelated = relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
			loginMemberId,
			kidId,
			MemberRole.KID
		);
		
		if (!isRelated) {
			throw new IllegalArgumentException("해당 계좌에 접근 권한이 없습니다.");
		}

		// 4. 전화번호 저장 이름 조회
		String phoneSavedName = phoneNameRepository
			.findNameByOwnerIdAndTargetId(loginMemberId, kidId)
			.orElse(null);

		// 5. 저장 이름이 없으면 실명만
		boolean hasPhoneSavedName = phoneSavedName != null && !phoneSavedName.isBlank();

		String displayName = hasPhoneSavedName
			? kid.getName() + "(" + phoneSavedName + ")"
			: kid.getName();

		// 6. 로그인 사용자 입출금 계좌 조회
		Account parentAccount = accountRepository
			.findByMemberIdAndAccountType(loginMemberId, AccountType.FREE)
			.orElseThrow(() -> new IllegalArgumentException("출금 계좌가 없습니다."));

		// 7. 잔액 조회
		BigDecimal balance = parentAccount.getBalance();

		// 8. DTO 반환
		return TransferPrepareResponseDto.builder()
			.accountId(accountId)
			.targetMemberName(kid.getName())
			.phoneSavedName(phoneSavedName)
			.displayName(displayName)
			.accountAlias(kidAccount.getName())
			.balance(balance)
			.build();
	}
}
