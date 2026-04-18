package com.hanaro.hanaconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.ai.AssetAiClient;
import com.hanaro.hanaconnect.dto.asset.AssetAIRecommendationResponseDTO;
import com.hanaro.hanaconnect.dto.asset.AssetSummaryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AssetAIService {

	private final AssetService assetService;
	private final AssetAiClient assetAiClient;
	private final ObjectMapper objectMapper;

	private final Map<String, AssetAIRecommendationResponseDTO> recommendationCache = new ConcurrentHashMap<>();

	private final List<String> investmentStyles = List.of("공격형", "중립형", "안정형");

	public AssetAIRecommendationResponseDTO getAssetRecommendation(Long memberId) {
		AssetSummaryResponseDTO summary = assetService.getMemberAssetSummary(memberId);
		BigDecimal realTotal = summary.getTotalAssets();

		String cacheKey = memberId + "_" + realTotal.toPlainString();

		// 캐시에 값이 있으면 즉시 반환 (AI 호출 안 함)
		if (recommendationCache.containsKey(cacheKey)) {
			return recommendationCache.get(cacheKey);
		}

		System.out.println("조회된 총 자산 금액: " + realTotal);

		System.out.println("AI 분석 시작 - 사용자 ID: " + memberId);
		System.out.println("조회된 총 자산 금액: " + realTotal);

		// 1. 투자 성향 선택 (memberId를 기반으로 고정된 성향을 선택하여 일관된 결과 제공)
		String fixedStyle = investmentStyles.get((int) (memberId % investmentStyles.size()));

		// 성향별 구체적인 가이드라인 설정
		String styleGuideline;
		switch (fixedStyle) {
			case "공격형":
				styleGuideline = "투자 비율을 전체 자산의 30%~50% 사이로 높게 설정하고, 공격적인 자산 증식을 제안해줘.";
				break;
			case "중립형":
				styleGuideline = "투자 비율을 15%~30% 사이로 설정하고, 안정성과 수익성의 균형을 맞춰줘.";
				break;
			case "안정형":
			default:
				styleGuideline = "투자 비율을 15% 미만으로 낮게 잡고, 예적금 위주의 보수적인 자산 운용을 제안해줘.";
				break;
		}

		String styleWithGuideline = fixedStyle + " (가이드라인: " + styleGuideline + ")";

		// 2. 프롬프트 생성
		String prompt = String.format("""
          사용자 현재 자산: 총 %s원 (예적금: %s, 입출금: %s, 투자: %s, 연금: %s).
          
          [절대 규칙 - 위반 시 무효]
		   1. 너가 추천하는 4가지 항목(recommendedDepositSavings, recommendedDepositWithdrawal, recommendedInvestment, recommendedPension)의 **산술적 합계는 반드시 정확히 %s원**이어야 해.
		   2. 이전의 모든 연산 결과나 데이터는 완전히 잊고, 오직 현재 자산인 %s원을 기준으로만 새롭게 분배해줘.
		   3. 자산이 변동되었으므로, 과거의 추천 비율에 얽매이지 말고 '지금 이 금액'에 가장 적합한 설계를 다시 해줘.
				         
          [미션]
          1. 사용자의 투자 성향이 '%s'이라고 가정하고, 현재 총 자산(%s원)을 유지한 채 4가지 항목(예적금, 입출금, 투자, 연금)에 대한 최적의 분배 금액을 결정해줘.
          2. 각 항목의 합은 반드시 총 자산(%s원)과 일치해야 해.
          3. 자산 안정성을 고려해 아이 용돈 비율(recommendRatio, 예: 10:90, 12:88, 20:80 등 다양하게)과 금액(kidAllowance) 추천.
          4. 자산 증가율(increaseRate, 5~15 사이 정수) 결정.
          5. 사용자의 총 자산(%s원)을 고려하여 적정 한 달 생활비를 산출할 것.
		  6. **응답 JSON의 "aiComment"는 반드시 아래 형식을 엄격히 지켜줘:**
		     "성향(%s)을 고려한 분석입니다. 이번 달 생활비는 [산출된 생활비]원이고, 자산 분배 결과 [추천비율]을 추천드려요. 직접 입력하거나 슬라이더를 옮겨서 비율을 조정해보세요!"
          7. **응답은 반드시 아래의 JSON 형식만 출력하고, 다른 설명이나 텍스트는 앞뒤에 절대 붙이지 마:**
          {
            "aiComment": "[위에서 지시한 문구]",
            "increaseRate": [숫자],
            "kidAllowance": [숫자],
            "recommendRatio": "[아이비율:부모비율]",
            "recommendedDepositSavings": [금액],
            "recommendedDepositWithdrawal": [금액],
            "recommendedInvestment": [금액],
            "recommendedPension": [금액]
          }
          8. **중요: 모든 비율은 반드시 '10:90' 또는 '20:80'과 같은 [숫자:숫자] 형식으로만 작성해줘.**
          """,
			// 1~5번: 현재 자산 정보
			realTotal, summary.getDepositSavings(), summary.getDepositWithdrawal(), summary.getInvestment(), summary.getPension(),
			// 6~7번: 절대 규칙용 (realTotal)
			realTotal, realTotal,
			// 8~10번: 미션용 (styleWithGuideline, realTotal, realTotal)
			styleWithGuideline, realTotal, realTotal,
			// 11번: 생활비 산출용 (realTotal)
			realTotal,
			// 12번: 코멘트 형식용 (fixedStyle)
			fixedStyle);

		try {
			// AI 서버 호출
			String jsonResponse = assetAiClient.getAssetRecommendationFromAi(prompt);
			JsonNode root = objectMapper.readTree(jsonResponse);

			// 3. AI가 결정한 '동적' 데이터 추출 (값이 없으면 괄호 안의 기본값 사용)
			int rawRate = root.path("increaseRate").asInt(8);
			// 5% ~ 15% 사이로 범위를 강제하여 계산 안정성 확보
			int aiIncreaseRate = Math.max(5, Math.min(15, rawRate));

			// 추천 비율, 용돈 금액, AI 코멘트 추출
			String aiRatio = root.path("recommendRatio").asText("5:95");
			BigDecimal aiAllowance = new BigDecimal(root.path("kidAllowance").asText("50000"));
			String aiComment = root.path("aiComment").asText("자산을 분석하여 최적의 생활비와 용돈 비율을 계산했습니다.");

			BigDecimal recDeposit = new BigDecimal(root.path("recommendedDepositSavings").asText("0"));
			BigDecimal recWithdrawal = new BigDecimal(root.path("recommendedDepositWithdrawal").asText("0"));
			BigDecimal recInvestment = new BigDecimal(root.path("recommendedInvestment").asText("0"));
			BigDecimal recPension = new BigDecimal(root.path("recommendedPension").asText("0"));

			// 4. 추출된 메소드를 통해 과거 자산 히스토리(그래프 데이터) 생성
			List<BigDecimal> assetHistory = calculateAssetHistory(realTotal, aiIncreaseRate);

			// 5. 최종 DTO 빌드 및 반환
			AssetAIRecommendationResponseDTO response = AssetAIRecommendationResponseDTO.builder()
				.aiComment(aiComment)
				.recommendRatio(aiRatio)
				.assetHistory(assetHistory)
				.increaseRate(aiIncreaseRate)
				.kidAllowance(aiAllowance)
				.recommendedDepositSavings(recDeposit)
				.recommendedDepositWithdrawal(recWithdrawal)
				.recommendedInvestment(recInvestment)
				.recommendedPension(recPension)
				.totalAssets(realTotal)
				.build();

			// 결과를 캐시에 저장
			recommendationCache.put(cacheKey, response);
			return response;

		} catch (Exception e) {
			return getFallback(realTotal);
		}
	}

	// 현재 자산과 증가율을 바탕으로 4개월치 자산 히스토리를 계산하는 메소드
	private List<BigDecimal> calculateAssetHistory(BigDecimal realTotal, int aiIncreaseRate) {
		BigDecimal rateFactor = BigDecimal.ONE.add(
			BigDecimal.valueOf(aiIncreaseRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
		);
		BigDecimal prevMonth = realTotal.divide(rateFactor, 0, RoundingMode.HALF_UP);

		return List.of(
			prevMonth.multiply(new BigDecimal("0.90")).setScale(0, RoundingMode.HALF_UP),
			prevMonth.multiply(new BigDecimal("0.95")).setScale(0, RoundingMode.HALF_UP),
			prevMonth,
			realTotal
		);
	}

	private AssetAIRecommendationResponseDTO getFallback(BigDecimal total) {
		return AssetAIRecommendationResponseDTO.builder()
			.recommendRatio("10:90")
			.kidAllowance(new BigDecimal("50000"))
			.aiComment("안정적인 관리를 추천드립니다.")
			.assetHistory(List.of(total, total, total, total))
			.increaseRate(0)
			.totalAssets(total)
			.recommendedDepositSavings(total)
			.recommendedDepositWithdrawal(BigDecimal.ZERO)
			.recommendedInvestment(BigDecimal.ZERO)
			.recommendedPension(BigDecimal.ZERO)
			.build();
	}

	public void clearRecommendationCache(Long memberId) {
		recommendationCache.keySet().removeIf(key -> key.startsWith(memberId + "_"));
	}
}
