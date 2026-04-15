package com.hanaro.hanaconnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.dto.AccountVerifyRequestDTO;
import com.hanaro.hanaconnect.dto.AccountVerifyResponseDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddRequestDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddResponseDTO;
import com.hanaro.hanaconnect.dto.KidAccountListResponseDTO;
import com.hanaro.hanaconnect.dto.KidWalletDetailResponseDTO;
import com.hanaro.hanaconnect.dto.MyAccountResponseDTO;
import com.hanaro.hanaconnect.dto.SavingsDetailResponseDTO;
import com.hanaro.hanaconnect.dto.TerminatedAccountResponseDTO;
import com.hanaro.hanaconnect.service.AccountService;
import com.hanaro.hanaconnect.service.TransferService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "계좌 관련", description = "본인 계좌 등록, 내 계좌 조회, 아이 계좌 추가 API")
public class AccountController {

	private final AccountService accountService;
	private final TransferService transferService;

	@PostMapping("/accounts/link")
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

	@PostMapping("/accounts/verify")
	@Operation(
		summary = "본인 계좌 확인",
		description = "로그인한 사용자가 입력한 계좌번호를 확인합니다. 본인 소유 계좌이고 아직 등록되지 않은 경우에만 성공합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "계좌 확인 성공"),
		@ApiResponse(responseCode = "400", description = "계좌 정보 오류 또는 이미 등록된 계좌"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "500", description = "서버 내부 오류")
	})
	public ResponseEntity<CustomAPIResponse<AccountVerifyResponseDTO>> verifyMyAccount(
		@Parameter(hidden = true) @AuthenticationPrincipal TokenMemberPrincipal principal,
		@Valid @RequestBody AccountVerifyRequestDTO request
	) {
		AccountVerifyResponseDTO response = accountService.verifyMyAccount(principal.getMemberId(), request);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"계좌 확인이 완료되었습니다."
			)
		);
	}

	@GetMapping("/accounts/me")
	@Operation(
		summary = "내 연결 계좌 목록 조회",
		description = "로그인한 사용자가 연결(등록)한 본인 계좌 목록을 조회합니다. 만기 계좌는 제외되며, limit를 전달하면 최근 연결순으로 해당 개수만 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 연결 계좌 목록 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "500", description = "서버 내부 오류")
	})
	public ResponseEntity<CustomAPIResponse<List<MyAccountResponseDTO>>> getMyAccounts(
		@Parameter(hidden = true) @AuthenticationPrincipal TokenMemberPrincipal principal,
		@Parameter(description = "최근 연결순으로 조회할 최대 개수", example = "2")
		@RequestParam(required = false) Integer limit
	) {
		List<MyAccountResponseDTO> response = accountService.getMyAccounts(principal.getMemberId(), limit);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"내 연결 계좌 목록 조회에 성공했습니다."
			)
		);
	}

	@GetMapping("/accounts/kids")
	@Operation(
		summary = "아이 계좌 목록 조회",
		description = "로그인한 부모(조부모 포함) 사용자가 본인이 추가한 아이 계좌 목록을 조회합니다. 만기 계좌는 제외되며, limit를 전달하면 최근 추가순으로 해당 개수만 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "아이 계좌 목록 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "500", description = "서버 내부 오류")
	})
	public ResponseEntity<CustomAPIResponse<List<KidAccountListResponseDTO>>> getKidAccounts(
		@Parameter(hidden = true) @AuthenticationPrincipal TokenMemberPrincipal principal,
		@Parameter(description = "최근 추가순으로 조회할 최대 개수", example = "2")
		@RequestParam(required = false) Integer limit
	) {
		List<KidAccountListResponseDTO> response = accountService.getKidAccounts(principal.getMemberId(), limit);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"아이 계좌 목록 조회에 성공했습니다."
			)
		);
	}

	@PostMapping("/kids/{kidId}/accounts")
	@Operation(
		summary = "아이 계좌 추가",
		description = "로그인한 부모(조부모 포함) 사용자가 연결된 아이의 기존 계좌를 자신의 계정에 추가합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "아이 계좌 추가 성공"),
		@ApiResponse(responseCode = "400", description = "계좌 정보 오류 또는 중복 등록"),
		@ApiResponse(responseCode = "500", description = "서버 내부 오류")
	})
	public ResponseEntity<CustomAPIResponse<KidAccountAddResponseDTO>> addKidAccount(
		@Parameter(hidden = true) @AuthenticationPrincipal TokenMemberPrincipal principal,
		@PathVariable Long kidId,
		@Valid @RequestBody KidAccountAddRequestDTO request
	) {
		KidAccountAddResponseDTO response = accountService.addKidAccount(principal.getMemberId(), kidId, request);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(CustomAPIResponse.createSuccess(
				HttpStatus.CREATED.value(),
				response,
				"아이 계좌 추가가 완료되었습니다."
			));
	}

	@GetMapping("/accounts/terminated-savings")
	@Operation(
		summary = "나의 만기된 적금 계좌 목록 조회",
		description = "로그인한 사용자의 계좌 중 만기된(is_end=true) 적금(SAVINGS) 목록을 조회합니다."
	)
	public ResponseEntity<CustomAPIResponse<List<TerminatedAccountResponseDTO>>> getTerminatedSavings(
		@Parameter(hidden = true) @AuthenticationPrincipal TokenMemberPrincipal principal
	) {
		List<TerminatedAccountResponseDTO> response = accountService.getTerminatedSavings(principal.getMemberId());

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"만기된 적금 목록 조회에 성공했습니다."
			)
		);
	}

	@GetMapping("/accounts/terminated-savings/{accountId}")
	@Operation(
		summary = "만기된 적금 상세 내역 및 편지함 조회",
		description = "로그인한 사용자의 계좌 중 만기된 적금 계좌의 상세 거래 내역과 편지를 조회합니다. 입금액, 잔액, 발신자 정보 및 메시지를 포함합니다."
	)
	public ResponseEntity<CustomAPIResponse<SavingsDetailResponseDTO>> getSavingsDetail(
		@Parameter(hidden = true) @AuthenticationPrincipal TokenMemberPrincipal principal,
		@PathVariable Long accountId
	) {
		SavingsDetailResponseDTO response = transferService.getExpiredSavingsDetail(principal.getMemberId(), accountId);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"만기된 적금의 상세 내역 조회에 성공했습니다."
			)
		);
	}

	@GetMapping("/kids/{kidId}/linked-accounts")
	public ResponseEntity<CustomAPIResponse<KidWalletDetailResponseDTO>> getKidLinkedAccounts(
		@Parameter(hidden = true) @AuthenticationPrincipal TokenMemberPrincipal principal,
		@PathVariable Long kidId
	) {
		KidWalletDetailResponseDTO response =
			accountService.getKidLinkedAccounts(principal.getMemberId(), kidId);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"아이 지갑 및 연결 계좌 조회에 성공했습니다."
			)
		);
	}
}
