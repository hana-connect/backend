package com.hanaro.hanaconnect.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HouseHistoryItemDTO {
	private int year;
	private int level;
	private int totalCount;
	private LocalDate paidAt;
	@JsonProperty("isFirst")
	private boolean isFirst;
	private String reward;
}
