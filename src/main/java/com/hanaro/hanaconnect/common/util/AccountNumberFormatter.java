package com.hanaro.hanaconnect.common.util;

public final class AccountNumberFormatter {

	private AccountNumberFormatter() {
	}

	public static String normalize(String accountNumber) {
		if (accountNumber == null) {
			return null;
		}
		return accountNumber.trim();
	}

	public static String format(String accountNumber) {
		String normalized = normalize(accountNumber);

		if (normalized == null || normalized.length() != 11) {
			throw new IllegalArgumentException("계좌번호 형식이 올바르지 않습니다.");
		}

		return normalized.substring(0, 3) + "-"
			+ normalized.substring(3, 7) + "-"
			+ normalized.substring(7, 11);
	}
}
