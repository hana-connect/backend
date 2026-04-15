package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsDetailResponseDTO {
	private String productName;
	private String accountNumber;
	private List<SavingsTransactionDTO> transactions;
}
