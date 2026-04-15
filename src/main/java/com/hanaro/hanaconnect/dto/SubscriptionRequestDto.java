package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionRequestDto {

	@NotNull(message = "납입 금액은 필수입니다.")
	@Positive(message = "납입 금액은 0보다 커야 합니다.")
	@Digits(integer = 13, fraction = 0, message = "납입 금액은 정수만 입력 가능합니다.")
	private BigDecimal amount;

	@Min(value = 1, message = "선납 횟수는 1 이상이어야 합니다.")
	@Max(value = 24, message = "선납 횟수는 24 이하여야 합니다.")
	private Integer prepaymentCount; // 첫 납입이면 null

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 6, max = 6, message = "비밀번호는 6자리여야 합니다.")
	private String password;

	// 첫 납입 + 25만원 초과일 때만 사용
	// true  -> 초과분 리워드 계좌로 이동
	// false -> 전액 청약 계좌로 납입
	private Boolean transferExcessToReward;

}
