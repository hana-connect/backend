package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsTransactionDTO {
	private Long transactionId;
	private LocalDateTime date;
	private BigDecimal amount;
	private BigDecimal balance;
	private String message;
	private String senderName;
}
