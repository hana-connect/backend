package com.hanaro.hanaconnect.dto;

import com.hanaro.hanaconnect.entity.Account;

/**
 * 만기된 계좌 정보를 담는 응답 DTO
 */
public record TerminatedAccountResponse(
	String name,
	String accountNumber
) {
	public static TerminatedAccountResponse from(Account account) {
		return new TerminatedAccountResponse(
			account.getName(),
			account.getAccountNumber()
		);
	}
}
