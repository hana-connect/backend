package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionInfoResponseDto {

	private Long subscriptionId;
	private String accountNumber;
	private boolean hasPaidThisMonth; // 이번 달 납입 여부
	private BigDecimal alreadyPaidAmount; // 이번 달 이미 납입한 금액

	private String displayName;       // 김채현(김*현)
	private String accountNickname;    // 계좌별명
	private BigDecimal balance;
	private String rewardAccountName; // 리워드 계좌 이름
}
