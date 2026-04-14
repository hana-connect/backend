package com.hanaro.hanaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
