package com.hanaro.hanaconnect.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.dto.LoginRequestDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	private Long memberId;
	private String accessToken;

	@BeforeEach
	void setUp() throws Exception {
		// 테스트용 회원 생성
		Member member = memberRepository.save(Member.builder()
			.name("홍길동")
			.password(passwordEncoder.encode("123456"))
			.birthday(LocalDate.of(2000, 1, 1))
			.virtualAccount("09090987654")
			.walletMoney(BigDecimal.ZERO)
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build());
		memberId = member.getId();

		// 만기된 적금 계좌 생성
		accountRepository.save(Account.builder()
			.member(member)
			.name("만기 테스트 적금")
			.accountNumber("49494848474")
			.password("1234")
			.accountType(AccountType.SAVINGS)
			.balance(BigDecimal.valueOf(10000))
			.isEnd(true)
			.build());

		// 로그인하여 토큰 발급받기
		LoginRequestDTO loginRequest = new LoginRequestDTO();
		loginRequest.setMemberId(memberId);
		loginRequest.setPassword("123456");

		MvcResult result = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn();

		accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
	}

	@Test
	@DisplayName("나의 만기된 적금 목록 조회 API - 성공")
	void getTerminatedSavings_success() throws Exception {
		mvc.perform(get("/api/accounts/terminated-savings")
				.header("Authorization", "Bearer " + accessToken) // 토큰 주입!
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0].name").value("만기 테스트 적금"))
			.andExpect(jsonPath("$.data[0].accountNumber").value("494-9484-8474"))
			.andExpect(jsonPath("$.message").value("만기된 적금 목록 조회에 성공했습니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("인증되지 않은 사용자가 조회 시 401 에러")
	void getTerminatedSavings_fail_unauthorized() throws Exception {
		mvc.perform(get("/api/accounts/terminated-savings")
				.contentType(MediaType.APPLICATION_JSON)) // 토큰 없이 호출
			.andExpect(status().isUnauthorized())
			.andDo(print());
	}

	@Test
	@DisplayName("만기된 적금 상세 내역 및 편지함 조회 - 성공")
	void getSavingsDetail_success() throws Exception {
		Account expiredAccount = accountRepository.findByMemberId(memberId).stream()
			.filter(Account::getIsEnd)
			.findFirst()
			.orElseThrow();

		Long accountId = expiredAccount.getId();

		mvc.perform(get("/api/accounts/terminated-savings/" + accountId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.productName").value("만기 테스트 적금"))
			.andExpect(jsonPath("$.data.accountNumber").value("49494848474"))
			.andExpect(jsonPath("$.data.transactions").isArray())
			.andExpect(jsonPath("$.message").value("만기된 적금의 상세 내역 조회에 성공했습니다."))
			.andDo(print());
	}
}
