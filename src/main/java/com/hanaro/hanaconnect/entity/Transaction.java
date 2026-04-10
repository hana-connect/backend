package com.hanaro.hanaconnect.entity;

import java.math.BigDecimal;

import com.hanaro.hanaconnect.common.entity.BaseEntity;
import com.hanaro.hanaconnect.common.enums.TransactionType;

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
@Table(name = "transaction")
public class Transaction extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(name = "transaction_money", nullable = false, precision = 15, scale = 2)
	private BigDecimal transactionMoney;

	@Column(name = "transaction_balance", nullable = false, precision = 15, scale = 2)
	private BigDecimal transactionBalance;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 20)
	private TransactionType transactionType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_account_id", nullable = false)
	private Account senderAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver_account_id", nullable = false)
	private Account receiverAccount;
}
