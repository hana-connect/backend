package com.hanaro.hanaconnect.common.util;

public final class KoreanFormatter {

	private KoreanFormatter() {
	}

	public static String getSubjectParticle(String word) {
		if (word == null || word.isBlank()) {
			return "이";
		}

		char lastChar = word.charAt(word.length() - 1);

		if (!isKoreanSyllable(lastChar)) {
			return "이";
		}

		return hasBatchim(lastChar) ? "이" : "가";
	}

	private static boolean isKoreanSyllable(char ch) {
		return ch >= 0xAC00 && ch <= 0xD7A3;
	}

	private static boolean hasBatchim(char ch) {
		return (ch - 0xAC00) % 28 != 0;
	}
}
