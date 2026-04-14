package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import com.hanaro.hanaconnect.common.enums.AccountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MyAccountResponseDTO {

	private Long accountId;
	private String name;
	private String accountNumber;
	private BigDecimal balance;
	private AccountType accountType;
	private String createdAt;
}
