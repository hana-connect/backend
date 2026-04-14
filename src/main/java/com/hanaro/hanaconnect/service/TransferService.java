package com.hanaro.hanaconnect.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.dto.TransferRequestDto;
import com.hanaro.hanaconnect.dto.TransferResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferService {

	private final AccountRepository accountRepository;
	private final PhoneNameRepository phoneNameRepository;
	private final RelationRepository relationRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final TransactionRepository transactionRepository;

	// 송금 준비 조회
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

	// 송금 실행
	@Transactional
	public TransferResponseDto transfer(Long loginMemberId, TransferRequestDto request) {

		// 1. 부모(로그인 사용자) 조회
		Member parent = memberRepository.findById(loginMemberId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

		// 2. 부모 입출금 계좌 조회
		Account parentAccount = accountRepository
			.findByMemberIdAndAccountType(loginMemberId, AccountType.FREE)
			.orElseThrow(() -> new IllegalArgumentException("출금 계좌가 없습니다."));

		// 3. 아이 계좌 조회
		Account kidAccount = accountRepository.findById(request.getAccountId())
			.orElseThrow(() -> new IllegalArgumentException("대상 계좌가 존재하지 않습니다."));

		Member kid = kidAccount.getMember();

		// 4. 부모-자식 관계 검증
		boolean isRelated = relationRepository
			.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(loginMemberId, kid.getId(), MemberRole.KID);

		if (!isRelated) {
			throw new IllegalArgumentException("해당 계좌에 접근 권한이 없습니다.");
		}

		// 5. 간편 비밀번호 검증
		if (!passwordEncoder.matches(request.getPassword(), parent.getPassword())) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}

		// 6. 금액
		BigDecimal amount = request.getAmount();

		if (parentAccount.getBalance().compareTo(amount) < 0) {
			throw new IllegalArgumentException("잔액이 부족합니다.");
		}

		// 7. 잔액 변경
		parentAccount.withdraw(amount);
		kidAccount.deposit(amount);

		// 8. 거래내역 저장
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

		// 9. 응답
		return TransferResponseDto.builder()
			.accountNumber(kidAccount.getAccountNumber())
			.amount(amount)
			.transferDate(LocalDate.now())
			.build();
	}
}
