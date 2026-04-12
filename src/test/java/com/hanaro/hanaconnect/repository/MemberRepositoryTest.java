package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.entity.Member;

class MemberRepositoryTest extends BaseRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
	}

	private Member createMember(String name, String virtualAccount, MemberRole memberRole) {
		return Member.builder()
			.name(name)
			.password("123456")
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount(virtualAccount)
			.walletMoney(BigDecimal.ZERO)
			.memberRole(memberRole)
			.role(Role.USER)
			.build();
	}
}
