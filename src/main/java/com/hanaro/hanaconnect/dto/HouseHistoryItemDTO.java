package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HouseHistoryItemDTO {
	private int year;
	private int level;
	private int totalCount;
	private LocalDate paidAt;
	private boolean isFirst;
	private String reward;
}
