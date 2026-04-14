package com.hanaro.hanaconnect.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.dto.AssetSummaryResponseDTO;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssetService {

	private final LinkedAccountRepository linkedAccountRepository;

	@Transactional(readOnly = true)
	public AssetSummaryResponseDTO getMemberAssetSummary(Long memberId) {
		List<LinkedAccount> linkedAccounts = linkedAccountRepository.findAllByMemberId(memberId);

		// 연결된 계좌가 없는 경우 예외 처리
		if (linkedAccounts.isEmpty()) {
			throw new EntityNotFoundException("연결된 계좌가 없습니다.");
		}

		// 예금 + 적금 + 청약 (DEPOSIT, SAVINGS, SUBSCRIPTION) 합산
		BigDecimal depositSavings = sumBalanceByAccountTypes(
			linkedAccounts,
			List.of(AccountType.DEPOSIT, AccountType.SAVINGS, AccountType.SUBSCRIPTION), memberId
		);

		BigDecimal depositWithdrawal = sumBalanceByAccountType(linkedAccounts, AccountType.FREE, memberId);
		BigDecimal investment = sumBalanceByAccountType(linkedAccounts, AccountType.INVESTMENT, memberId);
		BigDecimal pension = sumBalanceByAccountType(linkedAccounts, AccountType.PENSION, memberId);

		BigDecimal totalAssets = depositSavings
			.add(depositWithdrawal)
			.add(investment)
			.add(pension);

		return AssetSummaryResponseDTO.builder()
			.depositSavings(depositSavings)
			.depositWithdrawal(depositWithdrawal)
			.investment(investment)
			.pension(pension)
			.totalAssets(totalAssets)
			.build();
	}

	// 단일 타입을 위한 헬퍼
	private BigDecimal sumBalanceByAccountType(List<LinkedAccount> linkedAccounts, AccountType type, Long memberId) {
		return linkedAccounts.stream()
			.map(LinkedAccount::getAccount)
			.filter(account -> account.getAccountType() == type)
			.filter(account -> account.getMember().getId().equals(memberId))
			.map(account -> account.getBalance())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// 리스트로 여러 타입을 한 번에 더하기 위한 헬퍼 (예적금용)
	private BigDecimal sumBalanceByAccountTypes(List<LinkedAccount> linkedAccounts, List<AccountType> types, Long memberId) {
		return linkedAccounts.stream()
			.map(LinkedAccount::getAccount)
			.filter(account -> types.contains(account.getAccountType()))
			.filter(account -> account.getMember().getId().equals(memberId))
			.map(account -> account.getBalance())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
