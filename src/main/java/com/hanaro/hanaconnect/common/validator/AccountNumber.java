package com.hanaro.hanaconnect.common.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AccountNumberValidator.class)
public @interface AccountNumber {

	String message() default "계좌번호는 11자리 숫자여야 합니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
