package com.hanaro.hanaconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.RelayResponseDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.dto.TransferRequestDto;
import com.hanaro.hanaconnect.dto.TransferResponseDto;
import com.hanaro.hanaconnect.service.TransferService;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
@Tag(name = "이체 관련", description = "적금, 송금, 청약 API")
public class TransferController {

	private final TransferService transferService;

	@PostMapping("/savings")
	@Operation(summary = "적금", description = "아이 계좌로 적금을 넣어줍니다.")
	public ResponseEntity<CustomAPIResponse<SavingsTransferResponseDTO>> transferToSavings(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@Valid @RequestBody SavingsTransferRequestDTO request
	) {
		SavingsTransferResponseDTO response = transferService.transferToChildSavings(principal.getMemberId(), request);

		return ResponseEntity.ok(CustomAPIResponse.createSuccess(
			HttpStatus.OK.value(),
			response,
			"적금 송금이 완료되었습니다."
		));
	}
    
	@GetMapping("/prepare")
	@Operation(summary = "송금 준비 조회", description = "아이 계좌를 기준으로 송금 화면 정보를 조회합니다.")
	public ResponseEntity<CustomAPIResponse<TransferPrepareResponseDto>> getTransferPrepare(
		@RequestParam Long accountId,
		@AuthenticationPrincipal TokenMemberPrincipal principal
	){
		TransferPrepareResponseDto response =
			transferService.getTransferPrepareInfo(principal.getMemberId(), accountId);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"송금 준비 조회에 성공했습니다."
			)
		);
	}

	@PostMapping
	@Operation(summary = "송금 실행", description = "아이 계좌로 송금을 실행합니다.")
	public ResponseEntity<CustomAPIResponse<TransferResponseDto>> transfer(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@Valid @RequestBody TransferRequestDto request
	) {
		TransferResponseDto response =
			transferService.transfer(principal.getMemberId(), request);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"송금이 완료되었습니다."
			)
		);
	}

	@GetMapping("/savings/relay")
	@Operation(summary = "적금 릴레이 내역 조회", description = "프론트 릴레이 화면에 필요한 계좌 정보와 편지 목록을 조회합니다.")
	public ResponseEntity<CustomAPIResponse<RelayResponseDTO>> getRelayData(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@RequestParam Long targetAccountId
	) {

		RelayResponseDTO response = transferService.getRelayHistory(principal.getMemberId(), targetAccountId);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"적금 편지 내역 조회에 성공했습니다."
			)
		);
	}

	@GetMapping("/savings/relay/recent")
	@Operation(summary = "적금 릴레이 최근 3건 조회", description = "메인 화면용 최근 편지 3건을 조회합니다.")
	public ResponseEntity<CustomAPIResponse<RelayResponseDTO>> getRecentRelayData(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@RequestParam Long targetAccountId
	) {
		RelayResponseDTO response = transferService.getRecentRelayHistory(principal.getMemberId(), targetAccountId);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"최근 적금 편지 3건 조회에 성공했습니다."
			)
		);
	}
}
