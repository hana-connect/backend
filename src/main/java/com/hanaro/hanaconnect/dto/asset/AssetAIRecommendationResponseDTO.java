package com.hanaro.hanaconnect.dto.asset;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetAIRecommendationResponseDTO {
	private String recommendRatio;
	private BigDecimal kidAllowance;
	private String aiComment;

	private List<BigDecimal> assetHistory;
	private Integer increaseRate;

	private BigDecimal recommendedDepositSavings;
	private BigDecimal recommendedDepositWithdrawal;
	private BigDecimal recommendedInvestment;
	private BigDecimal recommendedPension;
	private BigDecimal totalAssets;
}
