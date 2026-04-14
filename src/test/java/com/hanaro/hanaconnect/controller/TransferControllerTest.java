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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.service.TransferService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private TransferService transferService;

	@Test
	@DisplayName("적금 송금 성공")
	void transferToSavings_success() throws Exception {
		Long memberId = 1L;

		SavingsTransferRequestDTO request = createRequest();

		SavingsTransferResponseDTO response = SavingsTransferResponseDTO.builder()
			.transactionMoney(new BigDecimal("10000"))
			.transactionBalance(new BigDecimal("40000"))
			.message("적금 응원 편지")
			.build();

		given(transferService.transferToChildSavings(eq(memberId), any(SavingsTransferRequestDTO.class)))
			.willReturn(response);

		mvc.perform(post("/api/transfer/savings")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("적금 송금이 완료되었습니다."))
			.andExpect(jsonPath("$.data.transactionMoney").value(10000))
			.andExpect(jsonPath("$.data.transactionBalance").value(40000))
			.andExpect(jsonPath("$.data.message").value("적금 응원 편지"));
	}

	@Test
	@DisplayName("적금 송금 실패 - 계좌 비밀번호 불일치")
	void transferToSavings_fail_wrong_account_password() throws Exception {
		Long memberId = 1L;

		SavingsTransferRequestDTO request = createRequest();
		request.setPassword("000000");

		given(transferService.transferToChildSavings(eq(memberId), any(SavingsTransferRequestDTO.class)))
			.willThrow(new IllegalArgumentException("계좌 비밀번호가 일치하지 않습니다."));

		mvc.perform(post("/api/transfer/savings")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("계좌 비밀번호가 일치하지 않습니다."));
	}

	@Test
	@DisplayName("적금 송금 실패 - 금액이 0원 이하")
	void transferToSavings_fail_invalid_amount() throws Exception {
		Long memberId = 1L;

		SavingsTransferRequestDTO request = createRequest();
		request.setAmount(BigDecimal.ZERO);

		given(transferService.transferToChildSavings(eq(memberId), any(SavingsTransferRequestDTO.class)))
			.willThrow(new IllegalArgumentException("송금 금액은 0보다 커야 합니다."));

		mvc.perform(post("/api/transfer/savings")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("송금 금액은 0보다 커야 합니다."));
	}

	@Test
	@DisplayName("적금 송금 실패 - 인증되지 않은 사용자")
	void transferToSavings_fail_unauthenticated() throws Exception {
		SavingsTransferRequestDTO request = createRequest();

		mvc.perform(post("/api/transfer/savings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		verify(transferService, never()).transferToChildSavings(any(), any());
	}

	@Test
	@DisplayName("송금 준비 조회 성공")
	void getTransferPrepare_success() throws Exception {
		Long memberId = 1L;
		Long accountId = 10L;

		TransferPrepareResponseDto response = TransferPrepareResponseDto.builder()
			.accountId(accountId)
			.targetMemberName("홍길동")
			.phoneSavedName("우리 아들")
			.displayName("홍길동(우리 아들)")
			.accountAlias("아이 입출금 통장")
			.balance(new BigDecimal("800000"))
			.build();

		given(transferService.getTransferPrepareInfo(any(), any()))
			.willReturn(response);

		mvc.perform(get("/api/transfer/prepare")
				.param("accountId", String.valueOf(accountId))
				.with(authentication(createAuthentication(memberId))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("송금 준비 조회에 성공했습니다."))
			.andExpect(jsonPath("$.data.accountId").value(10))
			.andExpect(jsonPath("$.data.targetMemberName").value("홍길동"))
			.andExpect(jsonPath("$.data.phoneSavedName").value("우리 아들"))
			.andExpect(jsonPath("$.data.displayName").value("홍길동(우리 아들)"))
			.andExpect(jsonPath("$.data.accountAlias").value("아이 입출금 통장"))
			.andExpect(jsonPath("$.data.balance").value(800000));
	}

	private UsernamePasswordAuthenticationToken createAuthentication(Long memberId) {
		TokenMemberPrincipal principal = mock(TokenMemberPrincipal.class);
		given(principal.getMemberId()).willReturn(memberId);

		return new UsernamePasswordAuthenticationToken(principal, null, List.of());
	}

	private SavingsTransferRequestDTO createRequest() {
		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(2L);
		request.setAmount(new BigDecimal("10000"));
		request.setPassword("111111");
		request.setContent("적금 응원 편지");
		return request;
	}

	@Test
	@DisplayName("적금 릴레이 내역 조회 성공")
	void getRelayData_success() throws Exception {
		// Given
		Long memberId = 1L;
		Long targetAccountId = 10L;

		com.hanaro.hanaconnect.dto.RelayResponseDTO response = com.hanaro.hanaconnect.dto.RelayResponseDTO.builder()
			.productName("우리 아이 적금")
			.accountNumber("123-456-789")
			.history(List.of(
				com.hanaro.hanaconnect.dto.RelayHistoryDTO.builder()
					.id(1L)
					.amount(new BigDecimal("10000"))
					.message("응원한다!")
					.date(java.time.LocalDateTime.now())
					.build()
			))
			.build();

		given(transferService.getRelayHistory(eq(memberId), eq(targetAccountId)))
			.willReturn(response);

		// When + Then
		mvc.perform(get("/api/transfer/savings/relay")
				.param("targetAccountId", String.valueOf(targetAccountId))
				.with(authentication(createAuthentication(memberId))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("적금 편지 내역 조회에 성공했습니다."))
			.andExpect(jsonPath("$.data.productName").value("우리 아이 적금"))
			.andExpect(jsonPath("$.data.accountNumber").value("123-456-789"))
			.andExpect(jsonPath("$.data.history[0].message").value("응원한다!"))
			.andExpect(jsonPath("$.data.history[0].amount").value(10000));
	}
}
