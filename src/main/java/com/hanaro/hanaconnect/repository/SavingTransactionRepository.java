package com.hanaro.hanaconnect.repository;

import java.util.Optional;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.entity.Transaction;

public interface SavingTransactionRepository extends JpaRepository<Transaction, Long> {

	/**
	 * 특정 회원의 일일 송금 누적액 조회
	 */
	@Query("SELECT SUM(t.transactionMoney) FROM Transaction t " +
		"WHERE t.senderAccount.member.id = :memberId " +
		"AND t.transactionType = :type " +
		"AND CAST(t.createdAt AS localdate) = :date")
	Optional<BigDecimal> sumAmountByMemberAndTypeAndDate(
		@Param("memberId") Long memberId,
		@Param("type") TransactionType type,
		@Param("date") java.time.LocalDate date
	);
}
