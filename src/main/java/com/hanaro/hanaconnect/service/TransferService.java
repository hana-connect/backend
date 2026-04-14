package com.hanaro.hanaconnect.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferService {

	private final AccountRepository accountRepository;
	private final PhoneNameRepository phoneNameRepository;

	@Transactional(readOnly = true)
	public TransferPrepareResponseDto getTransferPrepareInfo(Long loginMemberId, Long accountId){

		//1. 아이 계좌 조회
		Account kidAccount = accountRepository.findById(accountId)
			.orElseThrow(()-> new IllegalArgumentException("존재하지 않는 계좌입니다."));

		// 2. 아이 회원 조회
		Member kid = kidAccount.getMember();

		// 3. 전화번호 저장 이름 조회
		String phoneSavedName = phoneNameRepository
			.findNameByOwnerIdAndTargetId(loginMemberId, kid.getId())
			.orElse(null);

		// 4. 저장 이름이 없으면 실명만
		String displayName = (phoneSavedName != null)
			? kid.getName() + "(" + phoneSavedName + ")"
			: kid.getName();

		// 5. 연결된 부모 입출금 계좌 조회
		Account parentAccount = accountRepository
			.findByMemberIdAndAccountType(loginMemberId, AccountType.FREE)
			.orElseThrow(() -> new IllegalArgumentException("출금 계좌가 없습니다."));

		// 6. 잔액 조회
		BigDecimal balance = parentAccount.getBalance();

		// 7. DTO 반환
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
