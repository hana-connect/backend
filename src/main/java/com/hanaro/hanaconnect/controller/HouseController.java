package com.hanaro.hanaconnect.controller;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.HouseStatusResponseDTO;
import com.hanaro.hanaconnect.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "청약리포트", description = "청약리포트 관련 API")
@RestController
@RequestMapping("/api/house")
@RequiredArgsConstructor
public class HouseController {

	private final HouseService houseService;

	@Operation(
		summary = "청약 집 상태 조회",
		description = """
			아이의 현재 청약 집 상태를 조회합니다.

			- 아이(KID) 본인 요청 시: `kidId` 없이 호출합니다.
			- 조부모(PARENT) 요청 시: 조회할 아이의 `kidId`가 필수입니다.
			- 조부모는 본인과 연결된 아이만 조회할 수 있습니다.
			- 청약이 없는 경우 `level=0`, `gauge=0`으로 반환됩니다.
			""",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "청약 상태 조회 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = HouseStatusResponseDTO.class),
				examples = {
					@ExampleObject(
						name = "청약 있음",
						value = """
							{
							  "status": 200,
							  "message": "청약 상태 조회 성공",
							  "data": {
							    "memberId": 2,
							    "level": 3,
							    "gauge": 33,
							    "totalCount": 28,
							    "monthlyPayment": 200000.00,
							    "startDate": "2024-01-25",
							    "message": "할머니가 쌓아주신 덕분에 벽돌이 점점 높아지고 있어요! 28개월 동안 한결같이 쌓인 마음이 우리 별돌이의 집을 든든하게 세우고 있어요."
							  }
							}
							"""
					),
					@ExampleObject(
						name = "청약 없음",
						value = """
							{
							  "status": 200,
							  "message": "청약 상태 조회 성공",
							  "data": {
							    "memberId": 1,
							    "level": 0,
							    "gauge": 0,
							    "totalCount": null,
							    "monthlyPayment": null,
							    "startDate": null,
							    "message": null
							  }
							}
							"""
					)
				}
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "조부모 요청인데 kidId를 입력하지 않은 경우",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					value = """
						{
						  "status": 400,
						  "message": "kidId는 필수입니다.",
						  "data": null
						}
						"""
				)
			)
		),
		@ApiResponse(
			responseCode = "403",
			description = "관계 없는 아이 조회 시도",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					value = """
						{
						  "status": 403,
						  "message": "접근 권한이 없습니다.",
						  "data": null
						}
						"""
				)
			)
		),
		@ApiResponse(
			responseCode = "404",
			description = "회원 정보를 찾을 수 없음",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					value = """
						{
						  "status": 404,
						  "message": "회원 정보를 찾을 수 없습니다.",
						  "data": null
						}
						"""
				)
			)
		)
	})
	@GetMapping("/status")
	public ResponseEntity<CustomAPIResponse<HouseStatusResponseDTO>> getHouseStatus(
		@AuthenticationPrincipal TokenMemberPrincipal principal,

		@Parameter(
			description = "조회할 아이 회원 ID. 아이 본인은 생략, 조부모는 필수",
			example = "2"
		)
		@RequestParam(required = false) Long kidId
	) {
		HouseStatusResponseDTO response = houseService.getHouseStatus(principal.getMemberId(), kidId);
		return ResponseEntity.ok(CustomAPIResponse.createSuccess(HttpStatus.OK.value(), response, "청약 상태 조회 성공"));
	}
}
