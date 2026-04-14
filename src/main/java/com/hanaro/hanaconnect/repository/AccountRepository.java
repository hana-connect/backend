package com.hanaro.hanaconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.entity.Account;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {

	// 계좌번호와 회원 ID로 조회
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.accountNumber = :accountNumber and a.member.id = :memberId")
	Optional<Account> findByAccountNumberAndMemberIdWithLock(
		@Param("accountNumber") String accountNumber,
		@Param("memberId") Long memberId
	);

	// 지갑 계좌 조회용
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.member.id = :memberId and a.accountType = :accountType")
	Optional<Account> findByMemberIdAndAccountTypeWithLock(
		@Param("memberId") Long memberId,
		@Param("accountType") AccountType accountType
	);

	// 대상 적금 계좌 조회용
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.id = :id")
	Optional<Account> findByIdWithLock(@Param("id") Long id);

}
