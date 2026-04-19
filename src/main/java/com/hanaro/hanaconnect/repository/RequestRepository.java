package com.hanaro.hanaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.Request;

public interface RequestRepository extends JpaRepository<Request, Long> {
}
