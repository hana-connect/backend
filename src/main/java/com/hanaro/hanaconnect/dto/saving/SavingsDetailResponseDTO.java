package com.hanaro.hanaconnect.dto.saving;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.dto.transfer.SenderInfoDTO;

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

	private boolean isLast;

	@JsonProperty("isLast")
	public boolean isLast() {
		return isLast;
	}
}
