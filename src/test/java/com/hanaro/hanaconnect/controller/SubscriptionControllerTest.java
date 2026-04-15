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
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.dto.SubscriptionRequestDto;
import com.hanaro.hanaconnect.dto.SubscriptionResponseDto;
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

	@MockitoBean
	private SubscriptionService subscriptionService;

	@Test
	@DisplayName("청약 납입 정보 조회 성공 - 이번 달 납입 이력 있음")
	void getSubscriptionPaymentInfo_success_hasPaidThisMonth() throws Exception {
		Member member = memberRepository.findByName("김청약")
			.orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
		Long memberId = member.getId();
		Long subscriptionId = 10L;

		SubscriptionInfoResponseDto response = new SubscriptionInfoResponseDto(
			subscriptionId,
			"999900001111",
			true,
			new BigDecimal("200000")
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
			.andExpect(jsonPath("$.data.alreadyPaidAmount").value(200000));
	}

	@Test
	@DisplayName("청약 납입 정보 조회 성공 - 이번 달 납입 이력 없음")
	void getSubscriptionPaymentInfo_success_notPaidThisMonth() throws Exception {
		Member member = memberRepository.findByName("홍길동")
			.orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
		Long memberId = member.getId();
		Long subscriptionId = 20L;

		SubscriptionInfoResponseDto response = new SubscriptionInfoResponseDto(
			subscriptionId,
			"77788889999",
			false,
			BigDecimal.ZERO
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
			.andExpect(jsonPath("$.data.alreadyPaidAmount").value(0));
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
		Member member = memberRepository.findByName("김엄마")
			.orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
		Long memberId = member.getId();
		Long subscriptionId = 3L;

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("100000"));
		request.setPrepaymentCount(null);
		request.setPassword("123456");
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
		Member member = memberRepository.findByName("김엄마")
			.orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
		Long memberId = member.getId();
		Long subscriptionId = 3L;

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(new BigDecimal("300000"));
		request.setPrepaymentCount(null);
		request.setPassword("123456");
		request.setTransferExcessToReward(null);

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
			.andExpect(jsonPath("$.message").value("청약 납입이 완료되었으며, 초과 금액은 리워드 계좌로 입금되었습니다."))			.andExpect(jsonPath("$.data.subscriptionId").value(3))
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
		Member member = memberRepository.findByName("김엄마")
			.orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
		Long memberId = member.getId();
		Long subscriptionId = 3L;

		SubscriptionRequestDto request = new SubscriptionRequestDto();
		request.setAmount(BigDecimal.ZERO); // 0원 -> validation 실패
		request.setPrepaymentCount(null);
		request.setPassword("123"); // 6자리 아님 -> validation 실패

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
		request.setPassword("123456");

		mvc.perform(post("/api/subscriptions/{subscriptionId}/payments", subscriptionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		verify(subscriptionService, never()).paySubscription(any(), any(), any());
	}

	private UsernamePasswordAuthenticationToken createAuthentication(Long memberId) {
		TokenMemberPrincipal principal = mock(TokenMemberPrincipal.class);
		given(principal.getMemberId()).willReturn(memberId);

		return new UsernamePasswordAuthenticationToken(principal, null, List.of());
	}
}
