package com.hanaro.hanaconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.SubscriptionInfoResponseDto;
import com.hanaro.hanaconnect.service.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
