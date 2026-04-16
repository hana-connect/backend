package com.hanaro.hanaconnect.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
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
	LinkedAccountRepository linkedAccountRepository;

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

	private String login(Long loginMemberId, String password) throws Exception {
		LoginRequestDTO loginRequest = new LoginRequestDTO();
		loginRequest.setMemberId(loginMemberId);
		loginRequest.setPassword(password);

		MvcResult result = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn();

		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
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
			.andExpect(jsonPath("$.data.accountNumber").value("494-9484-8474"))
			.andExpect(jsonPath("$.data.transactions").isArray())
			.andExpect(jsonPath("$.message").value("만기된 적금의 상세 내역 조회에 성공했습니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("본인 계좌 확인 API - 성공")
	void verifyMyAccount_success() throws Exception {
		accountRepository.save(Account.builder()
			.member(memberRepository.findById(memberId).orElseThrow())
			.name("내 청약 통장")
			.accountNumber("81818181818")
			.password(passwordEncoder.encode("1234"))
			.accountType(AccountType.SUBSCRIPTION)
			.balance(BigDecimal.valueOf(150000))
			.build());

		mvc.perform(post("/api/accounts/verify")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
						  "accountNumber": "81818181818"
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.accountNumber").value("818-1818-1818"))
			.andExpect(jsonPath("$.data.accountType").value("SUBSCRIPTION"))
			.andExpect(jsonPath("$.message").value("계좌 확인이 완료되었습니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("본인 계좌 확인 API - 계좌번호 형식이 잘못되면 400")
	void verifyMyAccount_fail_invalidAccountNumber() throws Exception {
		mvc.perform(post("/api/accounts/verify")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "accountNumber": "123"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("계좌번호는 11자리 숫자여야 합니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("본인 계좌 등록 API - 성공")
	void linkMyAccount_success() throws Exception {
		accountRepository.save(Account.builder()
			.member(memberRepository.findById(memberId).orElseThrow())
			.name("내 적금 통장")
			.accountNumber("73737373737")
			.password(passwordEncoder.encode("1234"))
			.accountType(AccountType.SAVINGS)
			.balance(BigDecimal.valueOf(250000))
			.build());

		mvc.perform(post("/api/accounts/link")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "accountNumber": "73737373737",
					  "accountPassword": "1234"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value(201))
			.andExpect(jsonPath("$.data.accountNumber").value("737-3737-3737"))
			.andExpect(jsonPath("$.message").value("계좌 연결이 완료되었습니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("본인 계좌 등록 API - 계좌 비밀번호 형식이 잘못되면 400")
	void linkMyAccount_fail_invalidPasswordFormat() throws Exception {
		mvc.perform(post("/api/accounts/link")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "accountNumber": "73737373737",
					  "accountPassword": "12"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("계좌 비밀번호는 4자리 숫자여야 합니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("내 연결 계좌 목록 조회 API - linked_account 기준으로 반환")
	void getMyAccounts_success_linkedAccountOnly() throws Exception {
		Member member = memberRepository.findById(memberId).orElseThrow();

		Account linkedAccount = accountRepository.save(Account.builder()
			.member(member)
			.name("연결된 적금")
			.accountNumber("91919191919")
			.password(passwordEncoder.encode("1234"))
			.accountType(AccountType.SAVINGS)
			.balance(BigDecimal.valueOf(250000))
			.build());

		accountRepository.save(Account.builder()
			.member(member)
			.name("연결 안 된 통장")
			.accountNumber("92929292929")
			.password(passwordEncoder.encode("1234"))
			.accountType(AccountType.FREE)
			.balance(BigDecimal.valueOf(10000))
			.build());

		LinkedAccount linked = linkedAccountRepository.save(LinkedAccount.builder()
			.account(linkedAccount)
			.member(member)
			.build());
		linked.setCreatedAtForInit(LocalDateTime.of(2026, 4, 15, 10, 0));

			mvc.perform(get("/api/accounts/me")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].name").value("연결된 적금"))
				.andExpect(jsonPath("$.data[0].accountNumber").value("919-1919-1919"))
				.andExpect(jsonPath("$.message").value("내 연결 계좌 목록 조회에 성공했습니다."))
				.andDo(print());
	}

	@Test
	@DisplayName("내 연결 계좌 목록 조회 API - 인증되지 않은 사용자는 401")
	void getMyAccounts_fail_unauthorized() throws Exception {
		mvc.perform(get("/api/accounts/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andDo(print());
	}

	@Test
	@DisplayName("아이 계좌 목록 조회 API - 부모가 추가한 아이 계좌만 반환")
		void getKidAccounts_success() throws Exception {
		Member parent = memberRepository.save(Member.builder()
			.name("김엄마")
			.password(passwordEncoder.encode("123456"))
			.birthday(LocalDate.of(1980, 1, 1))
			.virtualAccount("93939393939")
			.walletMoney(BigDecimal.ZERO)
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build());

		String parentAccessToken = login(parent.getId(), "123456");

		Member kid = memberRepository.findById(memberId).orElseThrow();
		Account kidAccount = accountRepository.save(Account.builder()
			.member(kid)
			.name("아이 청약 통장")
			.accountNumber("94949494949")
			.password(passwordEncoder.encode("1234"))
			.accountType(AccountType.SUBSCRIPTION)
			.balance(BigDecimal.valueOf(120000))
			.build());

		linkedAccountRepository.save(LinkedAccount.builder()
			.account(kidAccount)
			.member(parent)
			.nickname("민수 청약")
			.build());

		mvc.perform(get("/api/accounts/kids")
				.header("Authorization", "Bearer " + parentAccessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].nickname").value("민수 청약"))
			.andExpect(jsonPath("$.data[0].accountNumber").value("949-4949-4949"))
			.andExpect(jsonPath("$.message").value("아이 계좌 목록 조회에 성공했습니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("아이 계좌 목록 조회 API - 아이가 호출하면 403")
	void getKidAccounts_fail_forbiddenForKid() throws Exception {
		mvc.perform(get("/api/accounts/kids")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andDo(print());
	}

	@Test
	@DisplayName("아이 계좌 추가 API - 별명이 비어 있으면 400")
	void addKidAccount_fail_blankNickname() throws Exception {
		mvc.perform(post("/api/kids/{kidId}/accounts", memberId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "accountNumber": "11122223333",
					  "nickname": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("계좌 별명은 필수입니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("아이 계좌 추가 API - 별명이 12자를 넘으면 400")
	void addKidAccount_fail_tooLongNickname() throws Exception {
		mvc.perform(post("/api/kids/{kidId}/accounts", memberId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "accountNumber": "11122223333",
					  "nickname": "1234567890123"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("계좌 별명은 12자 이하여야 합니다."))
			.andDo(print());
	}
}
