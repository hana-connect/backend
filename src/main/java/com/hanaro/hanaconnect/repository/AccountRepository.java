package com.hanaro.hanaconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByAccountNumberAndMemberId(String accountNumber, Long memberId);
	Optional<Account> findByMemberIdAndAccountType(Long memberId, AccountType accountType);
}
