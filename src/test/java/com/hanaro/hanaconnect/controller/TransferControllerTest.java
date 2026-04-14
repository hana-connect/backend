package com.hanaro.hanaconnect.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;

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
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.service.TransferService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	TransferService transferService;

	@Test
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
			.andExpect(jsonPath("$.data.message").value("적금 응원 편지"))
			.andDo(print());
	}

	@Test
	void transferToSavings_fail_wrong_account_password() throws Exception {
		Long memberId = 1L;

		SavingsTransferRequestDTO request = createRequest();
		request.setAccountPassword("0000");

		given(transferService.transferToChildSavings(eq(memberId), any(SavingsTransferRequestDTO.class)))
			.willThrow(new IllegalArgumentException("계좌 비밀번호가 일치하지 않습니다."));

		mvc.perform(post("/api/transfer/savings")
				.with(authentication(createAuthentication(memberId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("계좌 비밀번호가 일치하지 않습니다."))
			.andDo(print());
	}

	@Test
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
			.andExpect(jsonPath("$.message").value("송금 금액은 0보다 커야 합니다."))
			.andDo(print());
	}

	@Test
	void transferToSavings_fail_unauthenticated() throws Exception {
		SavingsTransferRequestDTO request = createRequest();

		mvc.perform(post("/api/transfer/savings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andDo(print());

		verify(transferService, never()).transferToChildSavings(any(), any());
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
		request.setAccountPassword("1111");
		request.setContent("적금 응원 편지");
		return request;
	}
}
