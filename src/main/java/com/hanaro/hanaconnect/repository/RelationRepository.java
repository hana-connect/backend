package com.hanaro.hanaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.Relation;

public interface RelationRepository extends JpaRepository<Relation, Long> {

	boolean existsByParent_IdAndKid_Id(Long parentId, Long kidId);
}
