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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "계좌 관련", description = "본인 계좌 등록 API")
public class AccountController {

	private final AccountService accountService;

	@PostMapping("/link")
	@Operation(
		summary = "본인 계좌 등록",
		description = "로그인한 사용자가 하이픈 없는 11자리 계좌번호와 계좌 비밀번호를 입력해 계좌를 연결합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "계좌 연결 성공"),
		@ApiResponse(responseCode = "400", description = "계좌 정보 오류, 계좌 비밀번호 불일치, 중복 등록"),
		@ApiResponse(responseCode = "500", description = "서버 내부 오류")
	})
	public ResponseEntity<CustomAPIResponse<AccountLinkResponseDTO>> linkMyAccount(
		@Parameter(hidden = true) @AuthenticationPrincipal TokenMemberPrincipal principal,
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
