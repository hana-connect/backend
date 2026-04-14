package com.hanaro.hanaconnect.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.dto.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final LinkedAccountRepository linkedAccountRepository;

	public SubscriptionInfoResponseDto getSubscriptionPaymentInfo(Long memberId, Long subscriptionId) {

		Account subscriptionAccount = accountRepository.findById(subscriptionId)
			.orElseThrow(() -> new IllegalArgumentException("청약 계좌를 찾을 수 없습니다."));

		if (subscriptionAccount.getAccountType() != AccountType.SUBSCRIPTION) {
			throw new IllegalArgumentException("청약 계좌가 아닙니다.");
		}

		// 연결 계좌
		boolean isLinked = linkedAccountRepository.existsByAccountIdAndMemberId(subscriptionId, memberId);
		if (!isLinked) {
			throw new IllegalArgumentException("접근할 수 없는 청약 계좌입니다.");
		}

		LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
		LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

		BigDecimal alreadyPaidAmount = transactionRepository.sumMonthlyPaymentAmount(
			subscriptionId,
			startOfMonth,
			endOfMonth
		);

		boolean hasPaidThisMonth = alreadyPaidAmount.compareTo(BigDecimal.ZERO) > 0;

		return new SubscriptionInfoResponseDto(
			subscriptionAccount.getId(),
			subscriptionAccount.getAccountNumber(),
			hasPaidThisMonth,
			alreadyPaidAmount
		);
	}
}
