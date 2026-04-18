package com.hanaro.hanaconnect.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

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

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.common.security.JwtTokenProvider;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.service.AccountHashService;

import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AssetControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	LinkedAccountRepository linkedAccountRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	AccountCryptoService accountCryptoService;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	private AccountHashService accountHashService;

	private String accessToken;
	private Long memberId;

	@BeforeEach
	void setUp() {
		String encodedPassword = passwordEncoder.encode("123456");

		// 1. 회원 생성
		Member member = Member.builder()
			.name("테스트유저")
			.password(encodedPassword)
			.birthday(LocalDate.of(2000, 1, 1))
			.virtualAccount(accountCryptoService.encrypt("1234567890"))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build();

		memberRepository.save(member);

		Member savedMember = memberRepository.findAll().stream()
			.filter(m -> m.getName().equals("테스트유저"))
			.findFirst()
			.orElseThrow();

		this.memberId = savedMember.getId();

		// ===============================
		// 계좌 1
		// ===============================
		String rawAccount1 = "111-1111-1111";
		String normalizedAccount1 = AccountNumberFormatter.normalize(rawAccount1); // 수정된 normalize 사용

		Account account1 = Account.builder()
			.name("예금계좌")
			.accountNumber(accountCryptoService.encrypt(normalizedAccount1))
			.accountNumberHash(accountHashService.hash(normalizedAccount1))
			.password("1234")
			.accountType(com.hanaro.hanaconnect.common.enums.AccountType.DEPOSIT)
			.balance(new BigDecimal("1000000"))
			.member(savedMember)
			.build();

		// ===============================
		// 계좌 2
		// ===============================
		String rawAccount2 = "222-2222-2222";
		String normalizedAccount2 = AccountNumberFormatter.normalize(rawAccount2);

		Account account2 = Account.builder()
			.name("연금계좌")
			.accountNumber(accountCryptoService.encrypt(normalizedAccount2))
			.accountNumberHash(accountHashService.hash(normalizedAccount2))
			.password("1234")
			.accountType(com.hanaro.hanaconnect.common.enums.AccountType.PENSION)
			.balance(new BigDecimal("500000"))
			.member(savedMember)
			.build();

		accountRepository.save(account1);
		accountRepository.save(account2);

		// 연결
		linkedAccountRepository.save(
			LinkedAccount.builder()
				.account(account1)
				.member(savedMember)
				.build()
		);

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.account(account2)
				.member(savedMember)
				.build()
		);

		// JWT 생성
		TokenMemberPrincipal principal = new TokenMemberPrincipal(
			savedMember.getId(),
			savedMember.getName(),
			savedMember.getVirtualAccount(),
			savedMember.getMemberRole(),
			savedMember.getRole()
		);

		accessToken = jwtTokenProvider.createAccessToken(principal);
	}

	@Test
	void getAssetSummaryTest() throws Exception {
		mvc.perform(get("/api/assets/summary")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("자산 현황 조회가 성공적으로 완료되었습니다."))
			.andExpect(jsonPath("$.data.totalAssets").value(1500000))
			.andExpect(jsonPath("$.data.depositSavings").value(1000000))
			.andExpect(jsonPath("$.data.pension").value(500000))
			.andDo(print());
	}

	@Test
	void getAIRecommendationTest() throws Exception {
		mvc.perform(get("/api/assets/recommendation")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.recommendRatio").exists())
			.andExpect(jsonPath("$.data.kidAllowance").exists())
			.andExpect(jsonPath("$.data.aiComment").exists())
			.andExpect(jsonPath("$.data.assetHistory").isArray())
			.andDo(print());
	}
}
