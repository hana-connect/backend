package com.hanaro.hanaconnect.controller;

// import com.hanaro.hanaconnect.common.security.CustomUserDetails;
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

	/**
	 * GET /api/house/status?kidId={kidId}
	 * - KID 본인: kidId 생략
	 * - PARENT(조부모): kidId 필수
	 */
	@GetMapping("/status")
	public ResponseEntity<HouseStatusResponseDTO> getHouseStatus(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam(required = false) Long kidId
	) {
		HouseStatusResponseDTO response = houseService.getHouseStatus(userDetails, kidId);
		return ResponseEntity.ok(response);
	}
}
