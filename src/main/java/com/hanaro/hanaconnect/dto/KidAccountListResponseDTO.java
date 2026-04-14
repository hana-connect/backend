package com.hanaro.hanaconnect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "아이 계좌 목록 조회 응답")
public class KidAccountListResponseDTO {

	@Schema(description = "연결 계좌 ID", example = "301")
	private Long linkedAccountId;

	@Schema(description = "실제 계좌 ID", example = "11")
	private Long accountId;

	@Schema(description = "부모(조부모)가 지정한 계좌 별명", example = "채현이 청약")
	private String nickname;

	@Schema(description = "화면 표시용 계좌번호", example = "111-2222-3333")
	private String accountNumber;
}
