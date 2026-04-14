package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "청약 집 상태 조회 응답")
public class HouseStatusResponseDTO {

	@Schema(description = "조회 대상 아이 회원 ID", example = "2")
	private Long memberId;

	@Schema(description = "현재 집 레벨(0~15)", example = "3")
	private Integer level;

	@Schema(description = "현재 레벨 구간 내 납입 진행도(0~100)", example = "33")
	private Integer gauge;

	@Schema(description = "총 납입 회차", example = "28", nullable = true)
	private Integer totalCount;

	@Schema(description = "이번 달 납입 금액", example = "200000.00", nullable = true)
	private BigDecimal monthlyPayment;

	@Schema(description = "청약 시작일", example = "2024-01-25", nullable = true)
	private LocalDate startDate;

	@Schema(
		description = "레벨과 요청자 역할에 따라 생성된 안내 메시지",
		example = "할머니가 쌓아주신 덕분에 벽돌이 점점 높아지고 있어요! 28개월 동안 한결같이 쌓인 마음이 우리 별돌이의 집을 든든하게 세우고 있어요.",
		nullable = true
	)
	private String message;
}
