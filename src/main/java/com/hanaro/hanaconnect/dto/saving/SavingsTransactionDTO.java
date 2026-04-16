package com.hanaro.hanaconnect.dto.saving;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd", timezone = "Asia/Seoul")
	private LocalDateTime date;

	private BigDecimal amount;
	private BigDecimal balance;
	private String message;
	private String senderName;
	private Long senderId;
}
