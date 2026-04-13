package com.hanaro.hanaconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "본인 계좌 등록 응답")
public class AccountLinkResponseDTO {

	@Schema(
		description = "화면 표시용 계좌번호",
		example = "111-2222-3333"
	)
	private String accountNumber;

	@Schema(
		description = "계좌 연결일",
		example = "2026.04.07"
	)
	private String linkedAt;
}
