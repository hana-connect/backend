package com.hanaro.hanaconnect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.service.TransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;


import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
@Tag(name = "이체 관련", description = "적금 송금 및 일반 이체 API")
public class TransferController {

	private final TransferService transferService;

	@PostMapping("/savings")
	public ResponseEntity<CustomAPIResponse<SavingsTransferResponseDTO>> transferToSavings(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@RequestBody SavingsTransferRequestDTO request
	) {
		SavingsTransferResponseDTO response = transferService.transferToChildSavings(principal.getMemberId(), request);

		return ResponseEntity.ok(CustomAPIResponse.createSuccess(
			200,
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
}
