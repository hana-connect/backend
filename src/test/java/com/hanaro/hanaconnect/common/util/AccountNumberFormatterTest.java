package com.hanaro.hanaconnect.common.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountNumberFormatterTest {

	@Test
	@DisplayName("계좌번호 normalize 시 앞뒤 공백을 제거한다")
	void normalize_success_trim() {
		String result = AccountNumberFormatter.normalize(" 11122223333 ");

		assertThat(result).isEqualTo("11122223333");
	}

	@Test
	@DisplayName("계좌번호 normalize 시 null이면 null을 반환한다")
	void normalize_null() {
		assertThat(AccountNumberFormatter.normalize(null)).isNull();
	}

	@Test
	@DisplayName("계좌번호 format 시 하이픈 포함 형식으로 반환한다")
	void format_success() {
		String result = AccountNumberFormatter.format("11122223333");

		assertThat(result).isEqualTo("111-2222-3333");
	}

	@Test
	@DisplayName("계좌번호 format 시 공백이 포함되어도 정상 포맷팅한다")
	void format_success_withTrimmedValue() {
		String result = AccountNumberFormatter.format(" 11122223333 ");

		assertThat(result).isEqualTo("111-2222-3333");
	}

	@Test
	@DisplayName("계좌번호 format 시 null이면 예외가 발생한다")
	void format_fail_null() {
		assertThatThrownBy(() -> AccountNumberFormatter.format(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("계좌번호 형식이 올바르지 않습니다.");
	}

	@Test
	@DisplayName("계좌번호 format 시 길이가 11자리가 아니면 예외가 발생한다")
	void format_fail_invalidLength() {
		assertThatThrownBy(() -> AccountNumberFormatter.format("1234"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("계좌번호 형식이 올바르지 않습니다.");
	}
}
