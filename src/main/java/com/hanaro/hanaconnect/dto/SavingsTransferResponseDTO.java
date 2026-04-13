package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavingsTransferResponseDTO {
	private BigDecimal transactionMoney;   // 보낸 금액
	private BigDecimal transactionBalance; // 남은 잔액
	private String message;                // 보낸 편지 내용
}
