package com.hanaro.hanaconnect.dto;

import com.hanaro.hanaconnect.common.enums.AccountType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "아이 계좌 추가 응답")
public class KidAccountAddResponseDTO {

	@Schema(description = "아이 이름", example = "김아이")
	private String kidName;

	@Schema(description = "화면 표시용 계좌번호", example = "111-2222-3333")
	private String accountNumber;

	@Schema(description = "계좌 타입", example = "SUBSCRIPTION")
	private AccountType accountType;

	@Schema(description = "계좌 추가 요청일", example = "2026.04.07")
	private String requestDate;
}
