package com.hanaro.hanaconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.dto.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.entity.Relation;

public interface RelationRepository extends JpaRepository<Relation, Long> {

	@Query("""
    select new com.hanaro.hanaconnect.dto.ConnectMemberResponseDTO(
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
    select new com.hanaro.hanaconnect.dto.ConnectMemberResponseDTO(
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

	boolean existsByMember_IdAndConnectMember_Id(Long memberId, Long connectMemberId);
}
