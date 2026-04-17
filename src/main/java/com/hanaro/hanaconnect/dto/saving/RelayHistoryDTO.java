package com.hanaro.hanaconnect.dto.saving;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelayHistoryDTO {
	private Long letterId;

	@JsonIgnore
	private LocalDateTime date;

	private BigDecimal amount;
	private String message;

	@JsonProperty("date")
	public String getFormattedDate() {
		return date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
	}
}
