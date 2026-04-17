package com.hanaro.hanaconnect.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 만기된 계좌 정보를 담는 응답 DTO
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminatedAccountResponseDTO {
	@Getter
	private Long accountId;

	@Getter
	private String name;

	private String accountNumber;

	public static TerminatedAccountResponseDTO from(Account account, String decryptedAccountNumber) {
		return TerminatedAccountResponseDTO.builder()
			.accountId(account.getId())
			.name(account.getName())
			.accountNumber(decryptedAccountNumber)
			.build();
	}

	@JsonProperty("accountNumber")
	public String getAccountNumber() {
		return AccountNumberFormatter.format(this.accountNumber);
	}
}
