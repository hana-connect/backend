package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetAIRecommendationResponseDTO {
	private String recommendRatio; // 추천 비율 (예: "10:90")
	private BigDecimal kidAllowance; // 추천 용돈 금액
	private String aiComment;

	private List<BigDecimal> assetHistory;
	private Integer increaseRate;
}
