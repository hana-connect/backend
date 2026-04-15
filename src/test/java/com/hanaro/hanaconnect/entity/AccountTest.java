package com.hanaro.hanaconnect.entity;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hanaro.hanaconnect.common.enums.AccountType;

class AccountTest {

	@Test
	@DisplayName("출금 성공 시 잔액이 차감된다")
	void withdraw_success() {
		Account account = createAccount(new BigDecimal("10000"));

		account.withdraw(new BigDecimal("3000"));

		assertThat(account.getBalance()).isEqualByComparingTo("7000");
	}

	@Test
	@DisplayName("출금 금액이 null이면 예외가 발생한다")
	void withdraw_fail_nullAmount() {
		Account account = createAccount(new BigDecimal("10000"));

		assertThatThrownBy(() -> account.withdraw(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("출금 금액은 0보다 커야 합니다.");
	}

	@Test
	@DisplayName("출금 금액이 0 이하이면 예외가 발생한다")
	void withdraw_fail_nonPositiveAmount() {
		Account account = createAccount(new BigDecimal("10000"));

		assertThatThrownBy(() -> account.withdraw(BigDecimal.ZERO))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("출금 금액은 0보다 커야 합니다.");
	}

	@Test
	@DisplayName("잔액보다 많이 출금하면 예외가 발생한다")
	void withdraw_fail_insufficientBalance() {
		Account account = createAccount(new BigDecimal("10000"));

		assertThatThrownBy(() -> account.withdraw(new BigDecimal("15000")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("잔액이 부족합니다.");
	}

	@Test
	@DisplayName("입금 성공 시 잔액이 증가한다")
	void deposit_success() {
		Account account = createAccount(new BigDecimal("10000"));

		account.deposit(new BigDecimal("2500"));

		assertThat(account.getBalance()).isEqualByComparingTo("12500");
	}

	@Test
	@DisplayName("입금 금액이 null이면 예외가 발생한다")
	void deposit_fail_nullAmount() {
		Account account = createAccount(new BigDecimal("10000"));

		assertThatThrownBy(() -> account.deposit(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("입금 금액은 0보다 커야 합니다.");
	}

	@Test
	@DisplayName("입금 금액이 0 이하이면 예외가 발생한다")
	void deposit_fail_nonPositiveAmount() {
		Account account = createAccount(new BigDecimal("10000"));

		assertThatThrownBy(() -> account.deposit(new BigDecimal("-1")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("입금 금액은 0보다 커야 합니다.");
	}

	private Account createAccount(BigDecimal balance) {
		return Account.builder()
			.name("테스트 통장")
			.accountNumber("11122223333")
			.password("encoded")
			.accountType(AccountType.FREE)
			.balance(balance)
			.build();
	}
}
