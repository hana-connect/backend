package com.hanaro.hanaconnect.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
	Optional<Transaction> findTopByReceiverAccountIdAndTransactionTypeOrderByCreatedAtDesc(
		Long receiverAccountId,
		TransactionType transactionType
	);

	// 이번달 청약 내역 확인
	@Query("""
	select coalesce(sum(t.transactionMoney), 0)
	from Transaction t
	where t.receiverAccount.id = :subscriptionId
	  and t.transactionType = :type
	  and t.createdAt between :start and :end
""")
	BigDecimal sumMonthlyPaymentAmount(
		@Param("subscriptionId") Long subscriptionId,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end,
		@Param("type") TransactionType type
	);
}
