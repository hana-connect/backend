package com.hanaro.hanaconnect.dto;

import com.hanaro.hanaconnect.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 만기된 계좌 정보를 담는 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminatedAccountResponseDTO {

	private String name;
	private String accountNumber;

	public static TerminatedAccountResponseDTO from(Account account) {
		return TerminatedAccountResponseDTO.builder()
			.name(account.getName())
			.accountNumber(account.getAccountNumber())
			.build();
	}
}
