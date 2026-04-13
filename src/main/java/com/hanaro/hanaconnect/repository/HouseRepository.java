package com.hanaro.hanaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.House;

public interface HouseRepository extends JpaRepository<House, Long> {
}
