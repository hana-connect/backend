package com.hanaro.hanaconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
	Optional<Transaction> findTopByReceiverAccountIdAndTransactionTypeOrderByCreatedAtDesc(
		Long receiverAccountId,
		TransactionType transactionType
	);
}
