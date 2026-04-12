package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WalletResponseDTO {
	private BigDecimal walletMoney;
}
