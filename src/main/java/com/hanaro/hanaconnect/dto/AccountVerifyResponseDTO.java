package com.hanaro.hanaconnect.dto;

import com.hanaro.hanaconnect.common.enums.AccountType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "계좌 확인 응답")
public class AccountVerifyResponseDTO {

	@Schema(description = "화면 표시용 계좌번호", example = "111-2222-3333")
	private String accountNumber;

	@Schema(description = "계좌 타입", example = "SUBSCRIPTION")
	private AccountType accountType;
}
