package com.hanaro.hanaconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.ai.AssetAiClient;
import com.hanaro.hanaconnect.dto.AssetAIRecommendationResponseDTO;
import com.hanaro.hanaconnect.dto.AssetSummaryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetAIService {

	private final AssetService assetService;
	private final AssetAiClient assetAiClient;
	private final ObjectMapper objectMapper;

	public AssetAIRecommendationResponseDTO getAssetRecommendation(Long memberId) {
		// 1. 실제 DB 데이터 조회
		AssetSummaryResponseDTO summary = assetService.getMemberAssetSummary(memberId);
		BigDecimal realTotal = summary.getTotalAssets();

		// 2. 프롬프트 생성
		String prompt = String.format("""
			사용자 자산 상태: 총 %s원 (예적금: %s, 연금: %s).
			[미션]
			1. 자산 안정성을 고려해 아이 용돈 비율(recommendRatio)과 금액(kidAllowance) 추천.
			2. 자산 증가율(increaseRate, 5~15 사이 정수) 결정.
			3. 사용자의 총 자산(%s원)을 고려하여 적정 한 달 생활비를 산출할 것.
			4. **응답은 반드시 아래의 JSON 형식만 출력하고, 다른 설명이나 텍스트는 앞뒤에 절대 붙이지 마:**
			{
			  "aiComment": "이번 달 생활비는 [금액]원이고, 자산 분배 결과 [아이비율:부모비율]을 추천드려요. 직접 입력하거나 슬라이더를 옮겨서 비율을 조정해보세요!",
			  "increaseRate": [숫자],
			  "kidAllowance": [숫자],
			  "recommendRatio": "[아이비율:부모비율]"
			}
			5. **중요: 모든 비율은 반드시 '10:90' 또는 '20:80'과 같은 [숫자:숫자] 형식으로만 작성해줘.**
			""",
			realTotal,
			summary.getDepositSavings(),
			summary.getPension(),
			realTotal);

		try {
			// AI 서버 호출
			String jsonResponse = assetAiClient.getAssetRecommendationFromAi(prompt);
			JsonNode root = objectMapper.readTree(jsonResponse);

			// 3. AI가 결정한 '동적' 데이터 추출 (값이 없으면 괄호 안의 기본값 사용)
			int aiIncreaseRate = root.path("increaseRate").asInt(8);
			String aiRatio = root.path("recommendRatio").asText("5:95");
			BigDecimal aiAllowance = new BigDecimal(root.path("kidAllowance").asText("50000"));
			String aiComment = root.path("aiComment").asText("자산을 분석하여 최적의 생활비와 용돈 비율을 계산했습니다.");

			// 4. 그래프용 히스토리 계산
			BigDecimal rateFactor = new BigDecimal(1.0 + (aiIncreaseRate / 100.0));
			BigDecimal prevMonth = realTotal.divide(rateFactor, 0, RoundingMode.HALF_UP);

			List<BigDecimal> assetHistory = List.of(
				prevMonth.multiply(new BigDecimal("0.90")).setScale(0, RoundingMode.HALF_UP),
				prevMonth.multiply(new BigDecimal("0.95")).setScale(0, RoundingMode.HALF_UP),
				prevMonth,
				realTotal // 4월 데이터는 진짜 DB 값
			);

			// 5. 모든 값을 AI가 준 결과로 채워서 반환
			return AssetAIRecommendationResponseDTO.builder()
				.aiComment(aiComment)
				.recommendRatio(aiRatio)
				.assetHistory(assetHistory)
				.increaseRate(aiIncreaseRate)
				.kidAllowance(aiAllowance)
				.build();

		} catch (Exception e) {
			return getFallback(realTotal);
		}
	}

	private AssetAIRecommendationResponseDTO getFallback(BigDecimal total) {
		return AssetAIRecommendationResponseDTO.builder()
			.recommendRatio("10:90").kidAllowance(new BigDecimal("50000"))
			.aiComment("안정적인 관리를 추천드립니다.").assetHistory(List.of(total, total, total, total))
			.increaseRate(0).build();
	}
}
