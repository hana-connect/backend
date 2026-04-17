package com.hanaro.hanaconnect.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {

	private Long subscriptionId;
	private String subscriptionAccountNumber;
	private BigDecimal subscriptionAmount;

	private String rewardAccountNumber; // 없으면 null
	private BigDecimal rewardAmount;    // 없으면 0

	private Integer prepaymentCount;    // 첫 납입이면 null
	private LocalDate paidAt;           // yyyy-MM-dd
}
