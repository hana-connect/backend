package com.hanaro.hanaconnect.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelayResponseDTO {
	@Getter
	private String productNickname;

	private String accountNumber;

	@JsonProperty("accountNumber")
	public String getAccountNumber() {
		return AccountNumberFormatter.format(this.accountNumber);
	}

	@Getter
	private List<RelayHistoryDTO> history;
}
