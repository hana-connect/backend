package com.hanaro.hanaconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.dto.account.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.entity.Relation;

public interface RelationRepository extends JpaRepository<Relation, Long> {

	@Query("""
    select new com.hanaro.hanaconnect.dto.account.ConnectMemberResponseDTO(
        cm.id,
        cm.name,
        pn.whomName,
	    cm.memberRole
    )
    from Relation r
    join r.connectMember cm
    left join PhoneName pn
        on pn.who.id = :memberId
       and pn.whom.id = cm.id
    where r.member.id = :memberId
      and r.connectMemberRole = com.hanaro.hanaconnect.common.enums.MemberRole.PARENT
	""")
	List<ConnectMemberResponseDTO> findParents(@Param("memberId") Long memberId);

	@Query("""
    select new com.hanaro.hanaconnect.dto.account.ConnectMemberResponseDTO(
        cm.id,
        cm.name,
        pn.whomName,
	    cm.memberRole
    )
    from Relation r
    join r.connectMember cm
    left join PhoneName pn
        on pn.who.id = :memberId
       and pn.whom.id = cm.id
    where r.member.id = :memberId
      and r.connectMemberRole = com.hanaro.hanaconnect.common.enums.MemberRole.KID
	""")
	List<ConnectMemberResponseDTO> findKids(@Param("memberId") Long memberId);

	boolean existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
		Long memberId,
		Long connectMemberId,
		MemberRole connectMemberRole
	);

	boolean existsByMemberIdAndConnectMemberId(Long kidId, Long parentId);

	@Query("""
	select new com.hanaro.hanaconnect.dto.account.ConnectMemberResponseDTO(
		parent.id,
		parent.name,
		pn.whomName,
		parent.memberRole
	)
	from Relation r
	join r.connectMember parent
	left join PhoneName pn
		on pn.who.id = :memberId
		and pn.whom.id = parent.id
	where r.member.id = :kidId
	  and r.connectMemberRole = com.hanaro.hanaconnect.common.enums.MemberRole.PARENT
	  and parent.id <> :memberId
	""")
	List<ConnectMemberResponseDTO> findOtherParents(Long memberId, Long kidId);
}
