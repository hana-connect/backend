package com.hanaro.hanaconnect.dto.account;

import java.math.BigDecimal;

import com.hanaro.hanaconnect.common.enums.AccountType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KidLinkedAccountResponseDTO {
	private Long linkedAccountId;
	private Long accountId;
	private String nickname;
	private String name;
	private String accountNumber;
	private BigDecimal balance;
	private AccountType accountType;
}
