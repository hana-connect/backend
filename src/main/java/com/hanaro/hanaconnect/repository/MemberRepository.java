package com.hanaro.hanaconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
