package com.hanaro.hanaconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.PhoneName;

public interface PhoneNameRepository extends JpaRepository<PhoneName, Long> {
	Optional<PhoneName> findByWhoIdAndWhomId(Long whoId, Long whomId);
}
