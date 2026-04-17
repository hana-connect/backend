package com.hanaro.hanaconnect.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.hanaro.hanaconnect.common.entity.BaseEntity;
import com.hanaro.hanaconnect.common.enums.AccountType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "account",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"account_number_hash", "member_id"})
	}
)
public class Account extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(nullable = false, length = 31)
	private String name;

	@Column(name = "account_number", nullable = false, unique = true, length = 100)
	private String accountNumber;

	@Column(name = "account_number_hash", nullable = false, length = 64)
	private String accountNumberHash;

	@Column(nullable = false, length = 255)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_type", nullable = false, length = 20)
	private AccountType accountType;

	@Column(name = "daily_limit", precision = 15, scale = 2)
	private BigDecimal dailyLimit;

	@Column(name = "total_limit", precision = 15, scale = 2)
	private BigDecimal totalLimit;

	@Column(nullable = false, precision = 15, scale = 2)
	@Builder.Default
	private BigDecimal balance = BigDecimal.ZERO;

	@Column(length = 50)
	private String nickname;

	@Column(name = "is_reward", nullable = false)
	@Builder.Default
	private Boolean isReward = false;

	@Column(name = "is_end")
	@Builder.Default
	private Boolean isEnd = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	// 이 계좌를 누가 연결했는지
	@OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
	@Builder.Default
	private List<LinkedAccount> linkedMembers = new ArrayList<>();

	// 출금
	public void withdraw(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("출금 금액은 0보다 커야 합니다.");
		}
		if (this.balance.compareTo(amount) < 0) {
			throw new IllegalArgumentException("잔액이 부족합니다.");
		}
		this.balance = this.balance.subtract(amount);
	}

	// 입금
	public void deposit(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("입금 금액은 0보다 커야 합니다.");
		}
		this.balance = this.balance.add(amount);
	}
}
