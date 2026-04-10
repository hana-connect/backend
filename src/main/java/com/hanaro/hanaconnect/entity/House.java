package com.hanaro.hanaconnect.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

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
@Table(name = "house")
public class House extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(nullable = false)
	private Integer level;

	@Column(name = "total_count")
	private Integer totalCount;

	@Column(name = "monthly_payment", precision = 15, scale = 2)
	private BigDecimal monthlyPayment;

	@Column(name = "start_date")
	private LocalDate startDate;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false, unique = true)
	private Account account;
}
