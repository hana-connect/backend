package com.hanaro.hanaconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.service.AccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@PostMapping("/link")
	public ResponseEntity<CustomAPIResponse<AccountLinkResponseDTO>> linkMyAccount(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@Valid @RequestBody AccountLinkRequestDTO request
	) {
		AccountLinkResponseDTO response = accountService.linkMyAccount(principal.getMemberId(), request);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(CustomAPIResponse.createSuccess(
				HttpStatus.CREATED.value(),
				response,
				"계좌 연결이 완료되었습니다."
			));
	}
}
