package com.hanaro.hanaconnect.dto.account;

import com.hanaro.hanaconnect.common.validator.AccountNumber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "계좌 확인 요청")
public class AccountVerifyRequestDTO {

	@Schema(
		description = "계좌번호 11자리 숫자",
		example = "11122223333"
	)
	@NotBlank(message = "계좌번호는 필수입니다.")
	@AccountNumber
	private String accountNumber;
}
