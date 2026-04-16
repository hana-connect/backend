package com.hanaro.hanaconnect.dto.account;

import com.hanaro.hanaconnect.common.validator.AccountNumber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "본인 계좌 등록 요청")
public class AccountLinkRequestDTO {

	@Schema(
		description = "계좌번호 11자리 숫자",
		example = "11122223333"
	)
	@NotBlank(message = "계좌번호는 필수입니다.")
	@AccountNumber
	private String accountNumber;

	@Schema(
		description = "계좌 비밀번호 4자리",
		example = "1234"
	)
	@NotBlank(message = "계좌 비밀번호는 필수입니다.")
	@Pattern(regexp = "^\\d{4}$", message = "계좌 비밀번호는 4자리 숫자여야 합니다.")
	private String accountPassword;
}
