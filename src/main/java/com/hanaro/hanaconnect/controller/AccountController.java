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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
		description = "로그인한 사용자가 자신의 실제 계좌번호와 계좌 비밀번호를 입력해 계좌를 연결합니다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "계좌 연결 성공",
			content = @Content(
				schema = @Schema(implementation = CustomAPIResponse.class),
				examples = @ExampleObject(
					value = """
						{
						  "status": 201,
						  "data": {
						    "accountNumber": "111-2222-3333",
						    "linkedAt": "2026.04.07"
						  },
						  "message": "계좌 연결이 완료되었습니다."
						}
						"""
				)
			)
		),
		@ApiResponse(responseCode = "400", description = "계좌번호 형식 오류 또는 계좌 비밀번호 불일치"),
		@ApiResponse(responseCode = "404", description = "해당 계좌 번호를 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "이미 등록된 계좌"),
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
