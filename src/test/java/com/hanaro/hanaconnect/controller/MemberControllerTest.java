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
		String kidAccount = generateAccount();
		String parentAccount = generateAccount();

		Member kid = Member.builder()
			.name("김꼬마")
			.password(encodedPassword)
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount(accountCryptoService.encrypt(kidAccount))
			.walletMoney(new BigDecimal("50000"))
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build();

		Member parent = Member.builder()
			.name("김엄마")
			.password(encodedPassword)
			.birthday(LocalDate.of(1980, 5, 19))
			.virtualAccount(accountCryptoService.encrypt(parentAccount))
			.walletMoney(new BigDecimal("100000"))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build();

		kid = memberRepository.save(kid);
		parent = memberRepository.save(parent);

		kidId = kid.getId();
		parentId = parent.getId();

		// 관계 설정
		relationRepository.save(
			Relation.builder()
				.member(kid)
				.connectMember(parent)
				.connectMemberRole(parent.getMemberRole())
				.build()
		);

		// 전화 이름 설정
		phoneNameRepository.save(
			PhoneName.builder()
				.who(kid)
				.whom(parent)
				.whomName("우리 엄마")
				.build()
		);

		// JWT 직접 생성
		TokenMemberPrincipal principal = new TokenMemberPrincipal(
			kid.getId(),
			kid.getName(),
			kid.getVirtualAccount(),
			kid.getMemberRole(),
			kid.getRole()
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
			.andDo(print());
	}

	// 무작위 생성
	private String generateAccount() {
		return String.valueOf(accountSeq++);
	}
}
