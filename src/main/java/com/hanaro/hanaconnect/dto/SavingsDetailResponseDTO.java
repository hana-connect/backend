package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class SavingsDetailResponseDTO {

	@Getter
	private String productName;

	private String accountNumber;

	@JsonProperty("accountNumber")
	public String getAccountNumber() {
		return AccountNumberFormatter.format(this.accountNumber);
	}

	@Getter
	private List<SavingsTransactionDTO> transactions;

	@Getter
	private List<SenderInfoDTO> senders;

	@Getter
	private boolean isLast;
}
