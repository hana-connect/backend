package com.hanaro.hanaconnect.common.validator;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AccountNumberValidator implements ConstraintValidator<AccountNumber, String> {

	private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^\\d{11}$");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return false;
		}

		return ACCOUNT_NUMBER_PATTERN.matcher(value).matches();
	}
}
