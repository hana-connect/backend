package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavingsTransferRequestDTO {
	private Long targetAccountId;
	private BigDecimal amount;
	private String accountPassword;
	private String content;  // 적금 편지

}
