package com.hanaro.hanaconnect.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.hanaro.hanaconnect.dto.saving.SavingsDetailResponseDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransactionDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.saving.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.transfer.SenderInfoDTO;
import com.hanaro.hanaconnect.dto.transfer.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.dto.transfer.TransferRequestDto;
import com.hanaro.hanaconnect.dto.transfer.TransferResponseDto;
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
	@DisplayName("만기 적금 상세 내역 조회 성공 - 필터링 포함")
	void getSavingsDetail_success() throws Exception {
		// Given
		Long memberId = 1L;
		Long accountId = 10L;
		Long senderId = 5L;
		int page = 0;

		SavingsDetailResponseDTO response = SavingsDetailResponseDTO.builder()
			.productName("아이 적금 통장")
			.accountNumber("12345678901")
			.senders(List.of(new SenderInfoDTO(5L, "엄마")))
			.transactions(List.of(
				SavingsTransactionDTO.builder()
					.transactionId(100L)
					.senderId(senderId)
					.senderName("엄마")
					.amount(new BigDecimal("10000"))
					.message("사랑해!")
					.date(java.time.LocalDateTime.now().withNano(0))
					.build()
			))
			.build();

		given(transferService.getExpiredSavingsDetail(eq(memberId), eq(accountId), eq(page), eq(senderId)))
			.willReturn(response);

		// When + Then
		mvc.perform(get("/api/accounts/terminated-savings/{accountId}", accountId)
				.param("page", String.valueOf(page))
				.param("senderId", String.valueOf(senderId))
				.with(authentication(createAuthentication(memberId))))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.senders").isArray())
			.andExpect(jsonPath("$.data.senders[0].senderName").value("엄마"))
			.andExpect(jsonPath("$.data.transactions[0].senderName").value("엄마"))
			.andExpect(jsonPath("$.data.transactions[0].message").value("사랑해!"));
	}

	@Test
	@DisplayName("송금 준비 조회 실패 - 계좌가 존재하지 않음")
	void getTransferPrepare_fail_account_not_found() throws Exception {
		Long memberId = 1L;
		Long accountId = 999L;

		given(transferService.getTransferPrepareInfo(eq(memberId), eq(accountId)))
			.willThrow(new IllegalArgumentException("계좌가 존재하지 않습니다."));

		mvc.perform(get("/api/transfer/prepare")
				.param("accountId", String.valueOf(accountId))
				.with(authentication(createAuthentication(memberId))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("계좌가 존재하지 않습니다."));
	}

	@Test
	@DisplayName("송금 준비 조회 실패 - 인증되지 않은 사용자")
	void getTransferPrepare_fail_unauthenticated() throws Exception {
		mvc.perform(get("/api/transfer/prepare")
				.param("accountId", "10"))
			.andExpect(status().isUnauthorized());

		verify(transferService, never()).getTransferPrepareInfo(any(), any());
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 성공 - senderId 없이")
	void getSavingsDetail_success_without_senderId() throws Exception {
		Long memberId = 1L;
		Long accountId = 10L;
		int page = 0;

		SavingsDetailResponseDTO response = SavingsDetailResponseDTO.builder()
			.productName("아이 적금 통장")
			.accountNumber("12345678901")
			.senders(List.of(new SenderInfoDTO(5L, "엄마")))
			.transactions(List.of())
			.build();

		given(transferService.getExpiredSavingsDetail(eq(memberId), eq(accountId), eq(page), eq(null)))
			.willReturn(response);

		mvc.perform(get("/api/accounts/terminated-savings/{accountId}", accountId)
				.param("page", String.valueOf(page))
				.with(authentication(createAuthentication(memberId))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.productName").value("아이 적금 통장"));
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 계좌가 존재하지 않음")
	void getSavingsDetail_fail_account_not_found() throws Exception {
		Long memberId = 1L;
		Long accountId = 999L;

		given(transferService.getExpiredSavingsDetail(eq(memberId), eq(accountId), eq(0), eq(null)))
			.willThrow(new IllegalArgumentException("계좌가 존재하지 않습니다."));

		mvc.perform(get("/api/accounts/terminated-savings/{accountId}", accountId)
				.param("page", "0")
				.with(authentication(createAuthentication(memberId))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("계좌가 존재하지 않습니다."));
	}

	@Test
	@DisplayName("만기 적금 상세 내역 조회 실패 - 인증되지 않은 사용자")
	void getSavingsDetail_fail_unauthenticated() throws Exception {
		mvc.perform(get("/api/accounts/terminated-savings/{accountId}", 10L)
				.param("page", "0"))
			.andExpect(status().isUnauthorized());

		verify(transferService, never()).getExpiredSavingsDetail(any(), any(), anyInt(), any());
	}

	@Test
	@DisplayName("적금 송금 실패 - amount 누락")
	void transferToSavings_fail_amount_null() throws Exception {
		Long memberId = 1L;

		SavingsTransferRequestDTO request = createRequest();
		request.setAmount(null);

		mvc.perform(post("/api/transfer/savings")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("적금 송금 실패 - targetAccountId 누락")
	void transferToSavings_fail_targetAccountId_null() throws Exception {
		Long memberId = 1L;

		SavingsTransferRequestDTO request = createRequest();
		request.setTargetAccountId(null);

		mvc.perform(post("/api/transfer/savings")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("일반 송금 성공")
	void transfer_success() throws Exception {
		Long memberId = 1L;

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(2L)
			.amount(new BigDecimal("50000"))
			.password("111111")
			.build();

		TransferResponseDto response = TransferResponseDto.builder()
			.transferId(100L)
			.toAccountId(2L)
			.toAccountNumber("12345678901")
			.amount(new BigDecimal("50000"))
			.transferredAt(LocalDateTime.of(2026, 4, 18, 13, 0, 0))
			.build();

		given(transferService.transfer(eq(memberId), any(TransferRequestDto.class)))
			.willReturn(response);

		mvc.perform(post("/api/transfer")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("송금이 완료되었습니다."))
			.andExpect(jsonPath("$.data.transferId").value(100))
			.andExpect(jsonPath("$.data.toAccountId").value(2))
			.andExpect(jsonPath("$.data.toAccountNumber").value("12345678901"))
			.andExpect(jsonPath("$.data.amount").value(50000));
	}

	@Test
	@DisplayName("일반 송금 실패 - 인증되지 않은 사용자")
	void transfer_fail_unauthenticated() throws Exception {
		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(2L)
			.amount(new BigDecimal("50000"))
			.password("111111")
			.build();

		mvc.perform(post("/api/transfer")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		verify(transferService, never()).transfer(any(), any());
	}

	@Test
	@DisplayName("일반 송금 실패 - accountId 누락")
	void transfer_fail_accountId_null() throws Exception {
		Long memberId = 1L;

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(null)
			.amount(new BigDecimal("50000"))
			.password("111111")
			.build();

		mvc.perform(post("/api/transfer")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("일반 송금 실패 - amount 누락")
	void transfer_fail_amount_null() throws Exception {
		Long memberId = 1L;

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(2L)
			.amount(null)
			.password("111111")
			.build();

		mvc.perform(post("/api/transfer")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("일반 송금 실패 - amount가 0")
	void transfer_fail_amount_zero() throws Exception {
		Long memberId = 1L;

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(2L)
			.amount(BigDecimal.ZERO)
			.password("111111")
			.build();

		mvc.perform(post("/api/transfer")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("일반 송금 실패 - password 누락")
	void transfer_fail_password_null() throws Exception {
		Long memberId = 1L;

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(2L)
			.amount(new BigDecimal("50000"))
			.password(null)
			.build();

		mvc.perform(post("/api/transfer")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("일반 송금 실패 - password 길이 오류")
	void transfer_fail_password_size() throws Exception {
		Long memberId = 1L;

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(2L)
			.amount(new BigDecimal("50000"))
			.password("123")
			.build();

		mvc.perform(post("/api/transfer")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}


}
