package com.hanaro.hanaconnect.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "송금 요청 정보")
public class TransferRequestDto {

	@Schema(description = "대상 아이 계좌 ID", example = "1")
	@NotNull(message = "대상 계좌 ID는 필수입니다.")
	private Long accountId;

	@Schema(description = "송금 금액(숫자만 입력)", example = "5000")
	@NotNull(message = "송금 금액은 필수입니다.")
	@Positive(message = "송금 금액은 0보다 커야 합니다.") // 0보다 커야 한다
	private BigDecimal amount;

	@Schema(description = "간편 비밀번호 6자리", example = "123456")
	@NotBlank(message = "간편 비밀번호는 필수입니다.")
	@Size(min = 6, max = 6, message = "간편 비밀번호는 6자리여야 합니다.")
	private String password;
}
