package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import com.hanaro.hanaconnect.common.enums.AccountType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MyAccountResponseDTO {

	@Schema(description = "계좌 ID", example = "1")
	private Long accountId;

	@Schema(description = "상품명", example = "부모 저축 예금")
	private String name;

	@Schema(description = "화면 표시용 계좌번호", example = "222-3333-4444")
	private String accountNumber;

	@Schema(description = "잔액", example = "100000")
	private BigDecimal balance;

	@Schema(description = "계좌 타입", example = "DEPOSIT")
	private AccountType accountType;

	@Schema(description = "계좌 생성일", example = "2026.04.14")
	private String createdAt;
}
