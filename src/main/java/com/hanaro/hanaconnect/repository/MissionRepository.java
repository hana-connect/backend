package com.hanaro.hanaconnect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.Mission;

public interface MissionRepository extends JpaRepository<Mission, Long> {

	// 퀴즈 : 최신 성공 미션 10개
	List<Mission> findTop10ByKidIdAndIsCompletedTrueOrderByIdDesc(Long kidId);}
