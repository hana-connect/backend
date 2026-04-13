package com.hanaro.hanaconnect.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.security.AccountCryptoService;
import com.hanaro.hanaconnect.common.security.JwtTokenProvider;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemberControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	RelationRepository relationRepository;

	@Autowired
	PhoneNameRepository phoneNameRepository;

	@Autowired
	MissionRepository missionRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	AccountCryptoService accountCryptoService;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	private static long accountSeq = 10000000000L;

	private Long kidId;
	private Long parentId;

	private String accessToken;

	@BeforeEach
	void setUp() {
		String encodedPassword = passwordEncoder.encode("123456");

		Member me = Member.builder()
			.name("김꼬마")
			.password(encodedPassword)
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount(accountCryptoService.encrypt(generateAccount()))
			.walletMoney(new BigDecimal("50000"))
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build();

		Member parent1 = Member.builder()
			.name("김엄마")
			.password(encodedPassword)
			.birthday(LocalDate.of(1980, 5, 19))
			.virtualAccount(accountCryptoService.encrypt(generateAccount()))
			.walletMoney(new BigDecimal("100000"))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build();

		Member parent2 = Member.builder()
			.name("이할머니")
			.password(encodedPassword)
			.birthday(LocalDate.of(1952, 8, 31))
			.virtualAccount(accountCryptoService.encrypt(generateAccount()))
			.walletMoney(new BigDecimal("90000"))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build();

		Member kidFriend = Member.builder()
			.name("박친구")
			.password(encodedPassword)
			.birthday(LocalDate.of(2011, 3, 10))
			.virtualAccount(accountCryptoService.encrypt(generateAccount()))
			.walletMoney(new BigDecimal("30000"))
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build();

		me = memberRepository.save(me);
		parent1 = memberRepository.save(parent1);
		parent2 = memberRepository.save(parent2);
		kidFriend = memberRepository.save(kidFriend);

		relationRepository.save(Relation.builder()
			.member(me)
			.connectMember(parent1)
			.connectMemberRole(parent1.getMemberRole())
			.build());

		relationRepository.save(Relation.builder()
			.member(me)
			.connectMember(parent2)
			.connectMemberRole(parent2.getMemberRole())
			.build());

		relationRepository.save(Relation.builder()
			.member(me)
			.connectMember(kidFriend)
			.connectMemberRole(kidFriend.getMemberRole())
			.build());

		phoneNameRepository.save(PhoneName.builder()
			.who(me)
			.whom(parent1)
			.whomName("우리 엄마")
			.build());

		phoneNameRepository.save(PhoneName.builder()
			.who(me)
			.whom(parent2)
			.whomName("할머니")
			.build());

		phoneNameRepository.save(PhoneName.builder()
			.who(me)
			.whom(kidFriend)
			.whomName("친구")
			.build());

		TokenMemberPrincipal principal = new TokenMemberPrincipal(
			me.getId(),
			me.getName(),
			me.getVirtualAccount(),
			me.getMemberRole(),
			me.getRole()
		);

		accessToken = jwtTokenProvider.createAccessToken(principal);
	}

	@Test
	void getMyWalletTest() throws Exception {
		mvc.perform(get("/api/wallet")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("내 지갑 잔액 조회에 성공했습니다."))
			.andExpect(jsonPath("$.data").exists())
			.andDo(print());
	}

	@Test
	void getParentsTest() throws Exception {
		mvc.perform(get("/api/parents")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("부모 리스트 조회에 성공했습니다."))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].connectMemberRole").value("PARENT"))
			.andExpect(jsonPath("$.data[1].connectMemberRole").value("PARENT"))
			.andDo(print());
	}

	@Test
	void getKidsTest() throws Exception {
		mvc.perform(get("/api/kids")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("아이 리스트 조회에 성공했습니다."))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].connectMemberName").value("박친구"))
			.andExpect(jsonPath("$.data[0].connectMemberRole").value("KID"))
			.andDo(print());
	}

	// 무작위 생성
	private String generateAccount() {
		return String.valueOf(accountSeq++);
	}
}
