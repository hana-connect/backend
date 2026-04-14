package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TransferPrepareResponseDto {

	private Long accountId;
	private String targetMemberName;
	private String phoneSavedName;
	private String displayName;
	private String accountAlias; // 계좌 별명
	private BigDecimal balance;
}
