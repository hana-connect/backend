package com.hanaro.hanaconnect.dto.asset;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetSummaryResponseDTO {
	private BigDecimal depositSavings;
	private BigDecimal depositWithdrawal;
	private BigDecimal investment;
	private BigDecimal pension;
	private BigDecimal totalAssets;
}
