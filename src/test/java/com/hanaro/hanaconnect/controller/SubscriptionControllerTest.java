package com.hanaro.hanaconnect.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionRequestDto;
import com.hanaro.hanaconnect.dto.subscription.SubscriptionResponseDto;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.service.SubscriptionService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private SubscriptionService subscriptionService;

	private Member parentMember;
	private Member kidMember;

	private static long accountSeq = 99900000000L;

	@BeforeEach
	void setUp() {
		parentMember = createMember("김엄마", MemberRole.PARENT);
		kidMember = createMember("홍길동", MemberRole.KID);
	}

	@Test
	@DisplayName("청약 납입 정보 조회 성공 - 이번 달 납입 이력 있음")
	void getSubscriptionPaymentInfo_success_hasPaidThisMonth() throws Exception {
		Long memberId = kidMember.getId();
		Long subscriptionId = 10L;

		SubscriptionInfoResponseDto response = new SubscriptionInfoResponseDto(
			subscriptionId,
			"999900001111",
			true,
			new BigDecimal("200000"),
			"김청약(김청*)",
			"청약통장",
			new BigDecimal("500000"),
			"리워드 계좌"
		);

		given(subscriptionService.getSubscriptionPaymentInfo(eq(memberId), eq(subscriptionId)))
			.willReturn(response);

		mvc.perform(get("/api/subscriptions/{subscriptionId}/payments/info", subscriptionId)
				.with(authentication(createAuthentication(memberId))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 납입 정보 조회 성공"))
			.andExpect(jsonPath("$.data.subscriptionId").value(10))
			.andExpect(jsonPath("$.data.accountNumber").value("999900001111"))
			.andExpect(jsonPath("$.data.hasPaidThisMonth").value(true))
			.andExpect(jsonPath("$.data.alreadyPaidAmount").value(200000))
			.andExpect(jsonPath("$.data.displayName").value("김청약(김청*)"))
			.andExpect(jsonPath("$.data.accountNickname").value("청약통장"))
			.andExpect(jsonPath("$.data.balance").value(500000))
			.andExpect(jsonPath("$.data.rewardAccountName").value("리워드 계좌"));
	}

	@Test
	@DisplayName("청약 납입 정보 조회 성공 - 이번 달 납입 이력 없음")
	void getSubscriptionPaymentInfo_success_notPaidThisMonth() throws Exception {
		Long memberId = kidMember.getId();
		Long subscriptionId = 20L;

		SubscriptionInfoResponseDto response = new SubscriptionInfoResponseDto(
			subscriptionId,
			"77788889999",
			false,
			BigDecimal.ZERO,
			"홍길동(홍길*)",
			"청약통장",
			new BigDecimal("300000"),
			"리워드 계좌"
		);

		given(subscriptionService.getSubscriptionPaymentInfo(eq(memberId), eq(subscriptionId)))
			.willReturn(response);

		mvc.perform(get("/api/subscriptions/{subscriptionId}/payments/info", subscriptionId)
				.with(authentication(createAuthentication(memberId))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 납입 정보 조회 성공"))
			.andExpect(jsonPath("$.data.subscriptionId").value(20))
			.andExpect(jsonPath("$.data.accountNumber").value("77788889999"))
			.andExpect(jsonPath("$.data.hasPaidThisMonth").value(false))
			.andExpect(jsonPath("$.data.alreadyPaidAmount").value(0))
			.andExpect(jsonPath("$.data.displayName").value("홍길동(홍길*)"))
			.andExpect(jsonPath("$.data.accountNickname").value("청약통장"))
			.andExpect(jsonPath("$.data.balance").value(300000))
			.andExpect(jsonPath("$.data.rewardAccountName").value("리워드 계좌"));
	}

	@Test
	@DisplayName("청약 납입 정보 조회 실패 - 인증되지 않은 사용자")
	void getSubscriptionPaymentInfo_fail_unauthenticated() throws Exception {
		Long subscriptionId = 10L;

		mvc.perform(get("/api/subscriptions/{subscriptionId}/payments/info", subscriptionId))
			.andExpect(status().isUnauthorized());

		verify(subscriptionService, never()).getSubscriptionPaymentInfo(any(), any());
	}

	@Test
	@DisplayName("청약 납입 실행 성공")
	void paySubscription_success() throws Exception {
		Long memberId = parentMember.getId();
		Long subscriptionId = 3L;

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("100000"));
		request.setPrepaymentCount(null);
		request.setPassword("260420");
		request.setTransferExcessToReward(null);

		SubscriptionResponseDto response = SubscriptionResponseDto.builder()
			.subscriptionId(subscriptionId)
			.subscriptionAccountNumber("77788889999")
			.subscriptionAmount(new BigDecimal("100000"))
			.rewardAccountNumber(null)
			.rewardAmount(BigDecimal.ZERO)
			.prepaymentCount(null)
			.paidAt(java.time.LocalDate.of(2026, 4, 15))
			.build();

		given(subscriptionService.paySubscription(eq(memberId), eq(subscriptionId), any(SubscriptionRequestDto.class)))
			.willReturn(response);

		given(subscriptionService.createPaymentMessage(response))
			.willReturn("청약 납입이 완료되었습니다.");

		mvc.perform(post("/api/subscriptions/{subscriptionId}/payments", subscriptionId)
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 납입이 완료되었습니다."))
			.andExpect(jsonPath("$.data.subscriptionId").value(3))
			.andExpect(jsonPath("$.data.subscriptionAccountNumber").value("77788889999"))
			.andExpect(jsonPath("$.data.subscriptionAmount").value(100000))
			.andExpect(jsonPath("$.data.rewardAmount").value(0))
			.andExpect(jsonPath("$.data.prepaymentCount").doesNotExist())
			.andExpect(jsonPath("$.data.paidAt").value("2026-04-15"));
	}

	@Test
	@DisplayName("청약 납입 실행 성공 - 리워드 계좌 포함")
	void paySubscription_success_withReward() throws Exception {
		Long memberId = parentMember.getId();
		Long subscriptionId = 3L;

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("300000"));
		request.setPrepaymentCount(null);
		request.setPassword("260420");
		request.setTransferExcessToReward(true);

		SubscriptionResponseDto response = SubscriptionResponseDto.builder()
			.subscriptionId(subscriptionId)
			.subscriptionAccountNumber("77788889999")
			.subscriptionAmount(new BigDecimal("250000"))
			.rewardAccountNumber("22233336666")
			.rewardAmount(new BigDecimal("50000"))
			.prepaymentCount(null)
			.paidAt(java.time.LocalDate.of(2026, 4, 15))
			.build();

		given(subscriptionService.paySubscription(eq(memberId), eq(subscriptionId), any(SubscriptionRequestDto.class)))
			.willReturn(response);

		given(subscriptionService.createPaymentMessage(response))
			.willReturn("청약 납입이 완료되었으며, 초과 금액은 리워드 계좌로 입금되었습니다.");

		mvc.perform(post("/api/subscriptions/{subscriptionId}/payments", subscriptionId)
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 납입이 완료되었으며, 초과 금액은 리워드 계좌로 입금되었습니다."))
			.andExpect(jsonPath("$.data.subscriptionId").value(3))
			.andExpect(jsonPath("$.data.subscriptionAccountNumber").value("77788889999"))
			.andExpect(jsonPath("$.data.subscriptionAmount").value(250000))
			.andExpect(jsonPath("$.data.rewardAccountNumber").value("22233336666"))
			.andExpect(jsonPath("$.data.rewardAmount").value(50000))
			.andExpect(jsonPath("$.data.prepaymentCount").doesNotExist())
			.andExpect(jsonPath("$.data.paidAt").value("2026-04-15"));
	}

	@Test
	@DisplayName("청약 납입 실행 실패 - 요청값 검증 실패")
	void paySubscription_fail_validation() throws Exception {
		Long memberId = parentMember.getId();
		Long subscriptionId = 3L;

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(BigDecimal.ZERO);
		request.setPrepaymentCount(null);
		request.setPassword("123");

		mvc.perform(post("/api/subscriptions/{subscriptionId}/payments", subscriptionId)
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(subscriptionService, never()).paySubscription(any(), any(), any());
	}

	@Test
	@DisplayName("청약 납입 실행 실패 - 인증되지 않은 사용자")
	void paySubscription_fail_unauthenticated() throws Exception {
		Long subscriptionId = 3L;

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("100000"));
		request.setPrepaymentCount(null);
		request.setPassword("260420");

		mvc.perform(post("/api/subscriptions/{subscriptionId}/payments", subscriptionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		verify(subscriptionService, never()).paySubscription(any(), any(), any());
	}

	private Member createMember(String name, MemberRole memberRole) {
		String virtualAccount = String.valueOf(accountSeq++);

		return memberRepository.save(
			Member.builder()
				.name(name)
				.password(passwordEncoder.encode("260420"))
				.birthday(LocalDate.of(2000, 1, 1))
				.virtualAccount(virtualAccount)
				.memberRole(memberRole)
				.role(Role.USER)
				.build()
		);
	}

	private UsernamePasswordAuthenticationToken createAuthentication(Long memberId) {
		TokenMemberPrincipal principal = mock(TokenMemberPrincipal.class);
		given(principal.getMemberId()).willReturn(memberId);

		return new UsernamePasswordAuthenticationToken(principal, null, List.of());
	}
}
