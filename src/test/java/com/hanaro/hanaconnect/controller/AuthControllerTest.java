package com.hanaro.hanaconnect.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.login.LoginRequestDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	AccountCryptoService accountCryptoService;

	private static long accountSeq = 10000000000L;

	private Long memberId;

	@BeforeEach
	void setUp() {
		String encodedPassword = passwordEncoder.encode("123456");
		String account = generateAccount();

		Member member = Member.builder()
			.name("김꼬마")
			.password(encodedPassword)
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount(accountCryptoService.encrypt(account))
			.walletMoney(new BigDecimal("50000"))
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build();

		member = memberRepository.save(member);
		memberId = member.getId();
	}

	@Test
	void login_success() throws Exception {
		// given
		LoginRequestDTO request = new LoginRequestDTO();
		request.setMemberId(memberId);
		request.setPassword("123456");

		// when & then
		mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("로그인 성공"))
			.andExpect(jsonPath("$.data.accessToken").exists())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.memberId").value(memberId))
			.andExpect(jsonPath("$.data.name").value("김꼬마"))
			.andExpect(jsonPath("$.data.role").value("USER"))
			.andExpect(jsonPath("$.data.memberRole").value("KID"))
			.andDo(print());
	}

	@Test
	void login_fail_wrong_password() throws Exception {
		LoginRequestDTO request = new LoginRequestDTO();
		request.setMemberId(memberId);
		request.setPassword("000000");

		mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andDo(print());
	}

	@Test
	void login_fail_invalid_format() throws Exception {
		LoginRequestDTO request = new LoginRequestDTO();
		request.setMemberId(memberId);
		request.setPassword("123"); // 6자리 아닐 때

		mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").exists())
			.andDo(print());
	}

	private String generateAccount() {
		return String.valueOf(accountSeq++);
	}
}
