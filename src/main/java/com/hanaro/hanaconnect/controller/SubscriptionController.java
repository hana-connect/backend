package com.hanaro.hanaconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.dto.SubscriptionRequestDto;
import com.hanaro.hanaconnect.dto.SubscriptionResponseDto;
import com.hanaro.hanaconnect.service.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
@Tag(name = "청약 관련", description = "청약 API")
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	@GetMapping("/{subscriptionId}/payments/info")
	@Operation(summary = "청약 진입", description = "첫납입인지 확인")
	public ResponseEntity<CustomAPIResponse<SubscriptionInfoResponseDto>> getSubscriptionPaymentInfo(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@PathVariable Long subscriptionId
	){
		SubscriptionInfoResponseDto response =
			subscriptionService.getSubscriptionPaymentInfo(principal.getMemberId(), subscriptionId);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"청약 납입 정보 조회 성공"
			)
		);
	}

	@PostMapping("/{subscriptionId}/payments")
	@Operation(summary = "청약 납입 실행", description = "첫 납입인지, 25만원이상인지, 선납인지")
	public ResponseEntity<CustomAPIResponse<SubscriptionResponseDto>> paySubscription(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@PathVariable Long subscriptionId,
		@Valid @RequestBody SubscriptionRequestDto request
	){
		SubscriptionResponseDto response =
			subscriptionService.paySubscription(principal.getMemberId(), subscriptionId, request);

		String message = subscriptionService.createPaymentMessage(response);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				message
			)
		);
	}

}
