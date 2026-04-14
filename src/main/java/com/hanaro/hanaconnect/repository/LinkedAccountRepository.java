package com.hanaro.hanaconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.LinkedAccount;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

	boolean existsByAccountIdAndMemberId(Long accountId, Long memberId);
	List<LinkedAccount> findAllByMemberId(Long memberId);
}
