package com.hanaro.hanaconnect.dto;

import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.entity.Account;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RewardAccountResponseDTO {

	private Long accountId;
	private String name;
	private String accountNumber;

	public static RewardAccountResponseDTO from(Account account) {
		return RewardAccountResponseDTO.builder()
			.accountId(account.getId())
			.name(account.getName())
			.accountNumber(AccountNumberFormatter.format(account.getAccountNumber()))
			.build();
	}
}
