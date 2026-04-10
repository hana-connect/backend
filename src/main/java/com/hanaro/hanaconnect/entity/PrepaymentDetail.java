package com.hanaro.hanaconnect.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hanaro.hanaconnect.common.entity.BaseEntity;
import com.hanaro.hanaconnect.common.enums.Status;

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
@Table(name = "prepayment_detail",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"account_id", "round_no"})
	}
)
public class PrepaymentDetail extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(name = "round_no", nullable = false)
	private Integer roundNo;

	@Column(name = "due_month", nullable = false)
	private LocalDate dueMonth;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "prepayment_id", nullable = false)
	private Prepayment prepayment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;
}
