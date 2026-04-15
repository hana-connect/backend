package com.hanaro.hanaconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.entity.LinkedAccount;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

	boolean existsByAccountIdAndMemberId(Long accountId, Long memberId);
	List<LinkedAccount> findAllByMemberId(Long memberId);

	Optional<LinkedAccount> findByMemberIdAndAccountId(Long memberId, Long accountId);

	List<LinkedAccount> findByMemberIdAndAccount_Member_MemberRoleAndAccount_IsEndFalseOrderByCreatedAtDesc(
		Long memberId,
		MemberRole memberRole
	);

	List<LinkedAccount> findByMemberIdAndAccount_Member_IdAndAccount_IsEndFalseOrderByCreatedAtDesc(
		Long memberId,
		Long kidId
	);
}
