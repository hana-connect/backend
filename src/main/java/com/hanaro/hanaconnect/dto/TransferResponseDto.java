package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TransferResponseDto {

	private String accountNumber;   // 입금 계좌번호
	private BigDecimal amount;      // 송금 금액
	private LocalDate transferDate; // 송금일

}
