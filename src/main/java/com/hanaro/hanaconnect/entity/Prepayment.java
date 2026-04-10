package com.hanaro.hanaconnect.entity;

import java.math.BigDecimal;

import com.hanaro.hanaconnect.common.entity.BaseEntity;

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
@Table(name = "prepayment")
public class Prepayment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "installment_count", nullable = false)
	private Integer installmentCount;

	@Column(name = "installment_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal installmentAmount;

	@Column(name = "start_round", nullable = false)
	private Integer startRound;

	@Column(name = "end_round", nullable = false)
	private Integer endRound;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false, unique = true)
	private Account account;
}
