package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetSummaryResponse {
	private BigDecimal depositSavings;
	private BigDecimal depositWithdrawal;
	private BigDecimal investment;
	private BigDecimal pension;
	private BigDecimal totalAssets;
}
