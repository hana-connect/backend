package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KidWalletDetailResponseDTO {
	private Long kidId;
	private String kidName;
	private BigDecimal walletMoney;
	private List<KidLinkedAccountResponseDTO> accounts;
}
