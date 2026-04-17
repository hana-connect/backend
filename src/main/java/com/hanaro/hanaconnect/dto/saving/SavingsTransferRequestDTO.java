package com.hanaro.hanaconnect.dto.saving;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavingsTransferRequestDTO {

	@NotNull(message = "대상 계좌는 필수입니다.")
	private Long targetAccountId;

	@NotNull(message = "송금 금액은 필수입니다.")
	@Positive(message = "송금 금액은 0보다 커야 합니다.")
	private BigDecimal amount;

	@NotBlank(message = "간편비밀번호는 필수입니다.")
	@Size(min = 6, max = 6, message = "간편 비밀번호는 6자리여야 합니다.")
	private String password;

	private String content;  // 적금 편지
}
