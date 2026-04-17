package com.hanaro.hanaconnect.controller;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.asset.AssetAIRecommendationResponseDTO;
import com.hanaro.hanaconnect.service.AssetAIService;
import com.hanaro.hanaconnect.dto.asset.AssetSummaryResponseDTO;
import com.hanaro.hanaconnect.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "자산 관련", description = "자산 현황 조회 및 AI 추천 API")
public class AssetController {

	private final AssetService assetService;
	private final AssetAIService aiAssetService;

	@GetMapping("/summary")
	@Operation(summary = "전체 자산 현황 조회", description = "연결된 모든 계좌의 잔액을 합산하여 카테고리별로 반환합니다.")
	public ResponseEntity<CustomAPIResponse<AssetSummaryResponseDTO>> getAssetSummary(
		@AuthenticationPrincipal TokenMemberPrincipal principal
	) {
		AssetSummaryResponseDTO response = assetService.getMemberAssetSummary(principal.getMemberId());

		return ResponseEntity.ok(CustomAPIResponse.createSuccess(
			HttpStatus.OK.value(),
			response,
			"자산 현황 조회가 성공적으로 완료되었습니다."
		));
	}

	@GetMapping("/recommendation")
	@Operation(summary = "자산관리 AI 추천", description = "유저의 자산을 분석하여 맞춤형 자산관리 전략을 추천합니다.")
	public ResponseEntity<CustomAPIResponse<AssetAIRecommendationResponseDTO>> getAIRecommendation(
		@AuthenticationPrincipal TokenMemberPrincipal principal
	) {
		AssetAIRecommendationResponseDTO response = aiAssetService.getAssetRecommendation(principal.getMemberId());

		return ResponseEntity.ok(CustomAPIResponse.createSuccess(
			HttpStatus.OK.value(),
			response,
			response.getAiComment()
		));
	}
}
