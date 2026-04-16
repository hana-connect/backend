package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TransferResponseDto {

	private Long transferId;            // 송금 거래 ID
	private Long toAccountId;           // 수신 계좌 ID
	private String toAccountNumber;     // 수신 계좌번호
	private BigDecimal amount;          // 송금 금액
	private LocalDateTime transferredAt; // 송금 완료 시각
}
