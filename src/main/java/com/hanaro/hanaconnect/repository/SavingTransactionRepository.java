package com.hanaro.hanaconnect.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.hanaro.hanaconnect.entity.Transaction;

public interface SavingTransactionRepository extends JpaRepository<Transaction, Long> {

}
