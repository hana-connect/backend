package com.hanaro.hanaconnect.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hanaro.hanaconnect.common.security.JwtTokenProvider;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.service.TransferService;

@ActiveProfiles("test")
@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TransferService transferService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("송금 준비 조회 성공")
	void getTransferPrepare_success() throws Exception {
		Long loginMemberId = 1L;
		Long accountId = 10L;

		TokenMemberPrincipal principal = Mockito.mock(TokenMemberPrincipal.class);
		given(principal.getMemberId()).willReturn(loginMemberId);

		Authentication authentication =
			new UsernamePasswordAuthenticationToken(principal, null, List.of());

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

		mockMvc.perform(
				get("/api/transfer/prepare")
					.param("accountId", String.valueOf(accountId))
					.with(authentication(authentication))
			)
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
}
