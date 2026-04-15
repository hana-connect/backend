package com.hanaro.hanaconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.Prepayment;

public interface PrepaymentRepository extends JpaRepository<Prepayment, Long> {

	Optional<Prepayment> findByAccountId(Long accountId);

}
