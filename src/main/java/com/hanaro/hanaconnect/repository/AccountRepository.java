package com.hanaro.hanaconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.entity.Account;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {
	// 대상 적금 계좌 조회용
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.id = :id")
	Optional<Account> findByIdWithLock(@Param("id") Long id);

	Optional<Account> findByMemberIdAndAccountType(Long memberId, AccountType accountType);

	// 리워드 계좌 조회
	Optional<Account> findByMemberIdAndIsRewardTrue(Long memberId);

	// 리워드 계좌 변경
	@Modifying
	@Query("UPDATE Account a SET a.isReward = :isReward WHERE a.id = :accountId")
	void updateIsReward(@Param("accountId") Long accountId, @Param("isReward") boolean isReward);

	Optional<Account> findByMemberIdAndAccountTypeAndIsRewardFalse(Long memberId, AccountType accountType);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
	select a
	from Account a
	where a.member.id = :memberId
	  and a.accountType = :accountType
	  and a.isReward = false
	""")
	Optional<Account> findByMemberIdAndAccountTypeAndIsRewardFalseWithLock(
		@Param("memberId") Long memberId,
		@Param("accountType") AccountType accountType
	);

	// 만기된(isEnd=true) 적금(SAVINGS) 계좌 목록 조회
	List<Account> findByMemberIdAndAccountTypeAndIsEndTrueOrderByIdAsc(Long memberId, AccountType accountType);

	List<Account> findByMemberId(Long memberId);

	Optional<Account> findByAccountNumberHashAndMemberId(String accountNumberHash, Long memberId);

}
