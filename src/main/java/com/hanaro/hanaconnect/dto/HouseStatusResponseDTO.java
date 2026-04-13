package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HouseStatusResponseDTO {
	private Long memberId;
	private Integer level;
	private Integer gauge;
	private Integer totalCount;
	private BigDecimal monthlyPayment;
	private LocalDate startDate;
	private String message;
}
