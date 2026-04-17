package com.hanaro.hanaconnect.dto.account;

import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RewardAccountResponseDTO {

	private Long accountId;
	private String name;
	private String accountNumber;

	public static RewardAccountResponseDTO from(LinkedAccount linkedAccount, String decryptedAccountNumber) {
		Account account = linkedAccount.getAccount();

		return RewardAccountResponseDTO.builder()
			.accountId(linkedAccount.getId())
			.name(account.getName())
			.accountNumber(AccountNumberFormatter.format(decryptedAccountNumber))
			.build();
	}
}
