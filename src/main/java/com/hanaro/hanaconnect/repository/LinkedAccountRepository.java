package com.hanaro.hanaconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.entity.LinkedAccount;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

	boolean existsByAccountIdAndMemberId(Long accountId, Long memberId);
	List<LinkedAccount> findAllByMemberId(Long memberId);

	Optional<LinkedAccount> findByMemberIdAndAccountId(Long memberId, Long accountId);

	@EntityGraph(attributePaths = "account")
	List<LinkedAccount> findByMemberIdAndAccount_Member_IdAndAccount_IsEndFalseOrderByCreatedAtDesc(
		Long memberId,
		Long accountOwnerId
	);

	@EntityGraph(attributePaths = "account")
	List<LinkedAccount> findByMemberIdAndAccount_Member_IdAndAccount_IsEndFalseOrderByCreatedAtDesc(
		Long memberId,
		Long accountOwnerId,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "account")
	List<LinkedAccount> findByMemberIdAndAccount_Member_MemberRoleAndAccount_IsEndFalseOrderByCreatedAtDesc(
		Long memberId,
		MemberRole memberRole
	);

	@EntityGraph(attributePaths = "account")
	List<LinkedAccount> findByMemberIdAndAccount_Member_MemberRoleAndAccount_IsEndFalseOrderByCreatedAtDesc(
		Long memberId,
		MemberRole memberRole,
		Pageable pageable
	);

	@Query("""
    select la
    from LinkedAccount la
    join fetch la.account a
    where la.member.id = :parentId
      and a.member.id = :kidId
      and a.isEnd = false
    order by la.createdAt desc
	""")
	List<LinkedAccount> findKidLinkedAccounts(@Param("parentId") Long parentId,
		@Param("kidId") Long kidId);

	@EntityGraph(attributePaths = "account")
	Optional<LinkedAccount> findByMemberIdAndAccount_IsRewardTrue(Long memberId);
}
