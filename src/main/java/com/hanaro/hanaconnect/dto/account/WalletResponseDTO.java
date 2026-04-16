package com.hanaro.hanaconnect.dto.account;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WalletResponseDTO {
	private String name;
	private BigDecimal walletMoney;
}
