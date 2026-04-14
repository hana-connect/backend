package com.hanaro.hanaconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hanaro.hanaconnect.entity.PhoneName;

public interface PhoneNameRepository extends JpaRepository<PhoneName, Long> {

	@Query("""
		SELECT p.whomName
		FROM PhoneName p
		WHERE p.who.id = :whoId
		  AND p.whom.id = :whomId
	""")
	Optional<String> findNameByOwnerIdAndTargetId(Long whoId, Long whomId);
}
