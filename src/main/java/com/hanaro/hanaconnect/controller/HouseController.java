package com.hanaro.hanaconnect.controller;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.HouseStatusResponseDTO;
import com.hanaro.hanaconnect.service.HouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/house")
@RequiredArgsConstructor
public class HouseController {

	private final HouseService houseService;

	@GetMapping("/status")
	public ResponseEntity<CustomAPIResponse<HouseStatusResponseDTO>> getHouseStatus(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@RequestParam(required = false) Long kidId
	) {
		HouseStatusResponseDTO response = houseService.getHouseStatus(principal.getMemberId(), kidId);
		return ResponseEntity.ok(CustomAPIResponse.createSuccess(200, response, "청약 상태 조회 성공"));
	}
}
