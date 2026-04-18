package com.hanaro.hanaconnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.Status;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionRequestDto;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Prepayment;
import com.hanaro.hanaconnect.entity.PrepaymentDetail;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.PrepaymentDetailRepository;
import com.hanaro.hanaconnect.repository.PrepaymentRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

	private static final BigDecimal MAX_FIRST_PAYMENT_AMOUNT = new BigDecimal("250000");

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final LinkedAccountRepository linkedAccountRepository;
	private final PrepaymentRepository prepaymentRepository;
	private final PrepaymentDetailRepository prepaymentDetailRepository;
	private final PasswordEncoder passwordEncoder;
	private final MemberRepository memberRepository;
	private final PhoneNameRepository phoneNameRepository;
	private final AccountCryptoService accountCryptoService;

	// 청약 진입
	public SubscriptionInfoResponseDto getSubscriptionPaymentInfo(Long memberId, Long subscriptionId) {
		Account subscriptionAccount = accountRepository.findById(subscriptionId)
			.orElseThrow(() -> new IllegalArgumentException("청약 계좌를 찾을 수 없습니다."));

		if (subscriptionAccount.getAccountType() != AccountType.SUBSCRIPTION) {
			throw new IllegalArgumentException("청약 계좌가 아닙니다.");
		}

		LinkedAccount linkedAccount = linkedAccountRepository
			.findByAccountIdAndMemberId(subscriptionId, memberId)
			.orElseThrow(() -> new IllegalArgumentException("접근할 수 없는 청약 계좌입니다."));

		Member kid = subscriptionAccount.getMember();
		Long kidId = kid.getId();

		String phoneSavedName = phoneNameRepository
			.findNameByOwnerIdAndTargetId(memberId, kidId)
			.orElse(null);

		boolean hasPhoneSavedName = phoneSavedName != null && !phoneSavedName.isBlank();

		String displayName = hasPhoneSavedName
			? kid.getName() + "(" + phoneSavedName + ")"
			: kid.getName();

		LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
		LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

		BigDecimal alreadyPaidAmount = transactionRepository.sumMonthlyPaymentAmount(
			subscriptionId,
			startOfMonth,
			endOfMonth,
			TransactionType.SUBSCRIPTION
		);

		if (alreadyPaidAmount == null) {
			alreadyPaidAmount = BigDecimal.ZERO;
		}

		boolean hasPaidThisMonth = alreadyPaidAmount.compareTo(BigDecimal.ZERO) > 0;

		Account walletAccount = accountRepository
			.findByMemberIdAndAccountTypeAndIsRewardFalse(memberId, AccountType.WALLET)
			.orElseThrow(() -> new IllegalArgumentException("지갑 계좌를 찾을 수 없습니다."));

		Account rewardAccount = accountRepository
			.findByMemberIdAndIsRewardTrue(memberId)
			.orElse(null);

		String linkedNickname = linkedAccount.getNickname();
		String subscriptionDisplayName =
			(linkedNickname != null && !linkedNickname.isBlank())
				? linkedNickname
				: subscriptionAccount.getName();

		return new SubscriptionInfoResponseDto(
			subscriptionAccount.getId(),
			subscriptionAccount.getAccountNumber(),
			hasPaidThisMonth,
			alreadyPaidAmount,
			displayName,
			subscriptionDisplayName,
			walletAccount.getBalance(),
			rewardAccount != null ? rewardAccount.getName() : null
		);
	}

	// 청약 납입 실행
	@Transactional
	public SubscriptionResponseDto paySubscription(
		Long memberId,
		Long subscriptionId,
		SubscriptionRequestDto request
	) {
		Account subscriptionAccount = getSubscriptionAccount(subscriptionId);

		boolean isLinked = linkedAccountRepository.existsByAccountIdAndMemberId(subscriptionId, memberId);
		if (!isLinked) {
			throw new IllegalArgumentException("접근할 수 없는 청약 계좌입니다.");
		}

		Account walletAccount = getWalletAccount(memberId);

		validatePassword(memberId, request.getPassword());

		boolean hasPaidThisMonth = isPaidThisMonth(subscriptionId);
		int lastRoundNo = getLastRoundNo(subscriptionId);

		if (!hasPaidThisMonth && request.getPrepaymentCount() == null) {
			return handleFirstPayment(memberId, subscriptionAccount, walletAccount, lastRoundNo, request);
		}

		if (hasPaidThisMonth && request.getPrepaymentCount() != null) {
			return handlePrepayment(subscriptionAccount, walletAccount, lastRoundNo, request);
		}

		throw new IllegalArgumentException("잘못된 청약 납입 요청입니다.");
	}

	private Account getSubscriptionAccount(Long subscriptionId) {
		Account account = accountRepository.findById(subscriptionId)
			.orElseThrow(() -> new IllegalArgumentException("청약 계좌를 찾을 수 없습니다."));

		if (account.getAccountType() != AccountType.SUBSCRIPTION) {
			throw new IllegalArgumentException("청약 계좌가 아닙니다.");
		}

		return account;
	}

	private Account getWalletAccount(Long memberId) {
		return accountRepository.findByMemberIdAndAccountTypeAndIsRewardFalse(memberId, AccountType.WALLET)
			.orElseThrow(() -> new IllegalArgumentException("지갑 계좌를 찾을 수 없습니다."));
	}

	private void validatePassword(Long memberId, String rawPassword) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}
	}

	private boolean isPaidThisMonth(Long subscriptionId) {
		LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
		LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

		BigDecimal monthlyAmount = transactionRepository.sumMonthlyPaymentAmount(
			subscriptionId,
			startOfMonth,
			endOfMonth,
			TransactionType.SUBSCRIPTION
		);

		return monthlyAmount.compareTo(BigDecimal.ZERO) > 0;
	}

	private int getLastRoundNo(Long subscriptionId) {
		return prepaymentDetailRepository.findMaxRoundNoByAccountId(subscriptionId)
			.orElse(0);
	}

	// 첫 납입
	private SubscriptionResponseDto handleFirstPayment(
		Long memberId,
		Account subscriptionAccount,
		Account walletAccount,
		int lastRoundNo,
		SubscriptionRequestDto request
	) {
		BigDecimal totalAmount = request.getAmount();

		BigDecimal subscriptionAmount;
		BigDecimal rewardAmount = BigDecimal.ZERO;
		String rewardAccountNumber = null;

		boolean isOverMax = totalAmount.compareTo(MAX_FIRST_PAYMENT_AMOUNT) > 0;

		if (isOverMax && request.getTransferExcessToReward() == null) {
			throw new IllegalArgumentException("25만 원 초과 시 리워드 계좌 입금 여부를 선택해주세요.");
		}

		if (isOverMax) {
			if (Boolean.TRUE.equals(request.getTransferExcessToReward())) {
				subscriptionAmount = MAX_FIRST_PAYMENT_AMOUNT;
				rewardAmount = totalAmount.subtract(MAX_FIRST_PAYMENT_AMOUNT);
			} else {
				subscriptionAmount = totalAmount;
			}
		} else {
			subscriptionAmount = totalAmount;
		}

		walletAccount.withdraw(totalAmount);
		subscriptionAccount.deposit(subscriptionAmount);

		saveTransaction(
			walletAccount,
			subscriptionAccount,
			totalAmount,
			walletAccount.getBalance(),
			TransactionType.WITHDRAW
		);

		saveTransaction(
			walletAccount,
			subscriptionAccount,
			subscriptionAmount,
			subscriptionAccount.getBalance(),
			TransactionType.SUBSCRIPTION
		);

		if (rewardAmount.compareTo(BigDecimal.ZERO) > 0) {
			Account rewardAccount = accountRepository.findByMemberIdAndIsRewardTrue(memberId)
				.orElseThrow(() -> new IllegalArgumentException("리워드 계좌를 찾을 수 없습니다."));

			rewardAccount.deposit(rewardAmount);

			saveTransaction(
				walletAccount,
				rewardAccount,
				rewardAmount,
				rewardAccount.getBalance(),
				TransactionType.DEPOSIT
			);

			rewardAccountNumber = accountCryptoService.decrypt(rewardAccount.getAccountNumber());
		}

		int currentRound = lastRoundNo + 1;

		Prepayment prepayment = getOrCreatePrepayment(
			subscriptionAccount,
			subscriptionAmount,
			1,
			currentRound,
			currentRound
		);

		PrepaymentDetail detail = PrepaymentDetail.builder()
			.account(subscriptionAccount)
			.prepayment(prepayment)
			.roundNo(currentRound)
			.dueMonth(LocalDate.now().withDayOfMonth(1))
			.amount(subscriptionAmount)
			.status(Status.COMPLETED)
			.build();

		prepaymentDetailRepository.save(detail);

		return SubscriptionResponseDto.builder()
			.subscriptionId(subscriptionAccount.getId())
			.subscriptionAccountNumber(accountCryptoService.decrypt(subscriptionAccount.getAccountNumber()))
			.subscriptionAmount(subscriptionAmount)
			.rewardAccountNumber(rewardAccountNumber)
			.rewardAmount(rewardAmount)
			.prepaymentCount(null)
			.paidAt(LocalDate.now())
			.build();
	}

	// 선납
	private SubscriptionResponseDto handlePrepayment(
		Account subscriptionAccount,
		Account walletAccount,
		int lastRoundNo,
		SubscriptionRequestDto request
	) {
		BigDecimal amount = request.getAmount();
		Integer prepaymentCount = request.getPrepaymentCount();

		if (prepaymentCount == null || prepaymentCount <= 0) {
			throw new IllegalArgumentException("선납 횟수가 올바르지 않습니다.");
		}

		BigDecimal divisor = BigDecimal.valueOf(prepaymentCount);
		if (amount.remainder(divisor).compareTo(BigDecimal.ZERO) != 0) {
			throw new IllegalArgumentException("선납 금액은 선납 횟수로 정확히 나누어져야 합니다.");
		}

		BigDecimal installmentAmount = amount.divide(divisor, 2, RoundingMode.HALF_UP);

		walletAccount.withdraw(amount);
		subscriptionAccount.deposit(amount);

		saveTransaction(
			walletAccount,
			subscriptionAccount,
			amount,
			walletAccount.getBalance(),
			TransactionType.WITHDRAW
		);

		saveTransaction(
			walletAccount,
			subscriptionAccount,
			amount,
			subscriptionAccount.getBalance(),
			TransactionType.SUBSCRIPTION
		);

		int startRound = lastRoundNo + 1;
		int endRound = lastRoundNo + prepaymentCount;

		Prepayment prepayment = getOrCreatePrepayment(
			subscriptionAccount,
			amount,
			prepaymentCount,
			startRound,
			endRound
		);

		List<PrepaymentDetail> details = new ArrayList<>();

		for (int i = 0; i < prepaymentCount; i++) {
			int roundNo = startRound + i;

			PrepaymentDetail detail = PrepaymentDetail.builder()
				.account(subscriptionAccount)
				.prepayment(prepayment)
				.roundNo(roundNo)
				.dueMonth(LocalDate.now().withDayOfMonth(1).plusMonths(i + 1L))
				.amount(installmentAmount)
				.status(Status.SCHEDULED)
				.build();

			details.add(detail);
		}

		prepaymentDetailRepository.saveAll(details);

		return SubscriptionResponseDto.builder()
			.subscriptionId(subscriptionAccount.getId())
			.subscriptionAccountNumber(accountCryptoService.decrypt(subscriptionAccount.getAccountNumber()))
			.subscriptionAmount(amount)
			.rewardAccountNumber(null)
			.rewardAmount(BigDecimal.ZERO)
			.prepaymentCount(prepaymentCount)
			.paidAt(LocalDate.now())
			.build();
	}

	private void saveTransaction(
		Account senderAccount,
		Account receiverAccount,
		BigDecimal amount,
		BigDecimal balanceAfter,
		TransactionType transactionType
	) {
		Transaction transaction = Transaction.builder()
			.senderAccount(senderAccount)
			.receiverAccount(receiverAccount)
			.transactionMoney(amount)
			.transactionBalance(balanceAfter)
			.transactionType(transactionType)
			.build();

		transactionRepository.save(transaction);
	}

	private Prepayment getOrCreatePrepayment(
		Account subscriptionAccount,
		BigDecimal totalAmount,
		Integer installmentCount,
		Integer startRound,
		Integer endRound
	) {
		return prepaymentRepository.findByAccountId(subscriptionAccount.getId())
			.map(existing -> updatePrepayment(existing, totalAmount, installmentCount, startRound, endRound))
			.orElseGet(() -> createPrepayment(subscriptionAccount, totalAmount, installmentCount, startRound, endRound));
	}

	private Prepayment createPrepayment(
		Account subscriptionAccount,
		BigDecimal totalAmount,
		Integer installmentCount,
		Integer startRound,
		Integer endRound
	) {
		Prepayment prepayment = Prepayment.builder()
			.account(subscriptionAccount)
			.totalAmount(totalAmount)
			.installmentCount(installmentCount)
			.installmentAmount(totalAmount.divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP))
			.startRound(startRound)
			.endRound(endRound)
			.build();

		return prepaymentRepository.save(prepayment);
	}

	private Prepayment updatePrepayment(
		Prepayment prepayment,
		BigDecimal totalAmount,
		Integer installmentCount,
		Integer startRound,
		Integer endRound
	) {
		prepayment.update(
			totalAmount,
			installmentCount,
			totalAmount.divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP),
			startRound,
			endRound
		);
		return prepayment;
	}

	public String createPaymentMessage(SubscriptionResponseDto response) {

		if (response.getPrepaymentCount() != null) {
			return "청약 선납이 완료되었습니다.";
		}

		if (response.getRewardAmount() != null
			&& response.getRewardAmount().compareTo(BigDecimal.ZERO) > 0) {
			return "청약 납입이 완료되었으며, 초과 금액은 리워드 계좌로 입금되었습니다.";
		}

		return "청약 납입이 완료되었습니다.";
	}

	@Transactional(readOnly = true)
	public SubscriptionResponseDto getSubscriptionPaymentResult(Long memberId, Long subscriptionId) {
		Account subscriptionAccount = getSubscriptionAccount(subscriptionId);

		boolean isLinked = linkedAccountRepository.existsByAccountIdAndMemberId(
			subscriptionAccount.getId(), memberId
		);
		if (!isLinked) {
			throw new IllegalArgumentException("접근할 수 없는 청약 계좌입니다.");
		}

		Transaction latestSubscriptionTx = transactionRepository
			.findTopByReceiverAccountIdAndTransactionTypeOrderByCreatedAtDesc(
				subscriptionAccount.getId(),
				TransactionType.SUBSCRIPTION
			)
			.orElseThrow(() -> new IllegalArgumentException("최근 청약 납입 내역이 없습니다."));

		BigDecimal rewardAmount = BigDecimal.ZERO;
		String rewardAccountNumber = null;

		Optional<Account> rewardAccountOpt = accountRepository.findByMemberIdAndIsRewardTrue(memberId);

		if (rewardAccountOpt.isPresent()) {
			Account rewardAccount = rewardAccountOpt.get();

			LocalDateTime paidAt = latestSubscriptionTx.getCreatedAt();
			LocalDateTime start = paidAt.minusSeconds(3);
			LocalDateTime end = paidAt.plusSeconds(3);

			Optional<Transaction> rewardTxOpt = transactionRepository
				.findTopByReceiverAccountIdAndTransactionTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
					rewardAccount.getId(),
					TransactionType.DEPOSIT,
					start,
					end
				);

			if (rewardTxOpt.isPresent()) {
				Transaction rewardTx = rewardTxOpt.get();
				rewardAmount = rewardTx.getTransactionMoney();
				rewardAccountNumber = accountCryptoService.decrypt(rewardAccount.getAccountNumber());
			}
		}

		return SubscriptionResponseDto.builder()
			.subscriptionId(subscriptionAccount.getId())
			.subscriptionAccountNumber(
				accountCryptoService.decrypt(subscriptionAccount.getAccountNumber())
			)
			.subscriptionAmount(latestSubscriptionTx.getTransactionMoney())
			.rewardAccountNumber(rewardAccountNumber)
			.rewardAmount(rewardAmount)
			.prepaymentCount(null)
			.paidAt(latestSubscriptionTx.getCreatedAt().toLocalDate())
			.build();
	}
}
