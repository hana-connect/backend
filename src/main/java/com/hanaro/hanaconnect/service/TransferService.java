package com.hanaro.hanaconnect.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferService {

	private final AccountRepository accountRepository;
	private final PhoneNameRepository phoneNameRepository;
	private final RelationRepository relationRepository;

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
