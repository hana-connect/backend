package com.hanaro.hanaconnect.common.validator;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountNumberValidatorTest {

	private final AccountNumberValidator validator = new AccountNumberValidator();

	@Test
	@DisplayName("11자리 숫자 계좌번호는 유효하다")
	void isValid_success() {
		boolean result = validator.isValid("11122223333", null);

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("null 계좌번호는 유효하지 않다")
	void isValid_fail_null() {
		boolean result = validator.isValid(null, null);

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("공백 계좌번호는 유효하지 않다")
	void isValid_fail_blank() {
		boolean result = validator.isValid("   ", null);

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("10자리 계좌번호는 유효하지 않다")
	void isValid_fail_shortLength() {
		boolean result = validator.isValid("1112222333", null);

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("문자가 포함된 계좌번호는 유효하지 않다")
	void isValid_fail_nonNumeric() {
		boolean result = validator.isValid("11122A23333", null);

		assertThat(result).isFalse();
	}
}
