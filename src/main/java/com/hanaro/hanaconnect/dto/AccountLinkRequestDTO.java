package com.hanaro.hanaconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountLinkRequestDTO {

	@NotBlank(message = "계좌번호는 필수입니다.")
	private String accountNumber;

	@NotBlank(message = "계좌 비밀번호는 필수입니다.")
	@Pattern(regexp = "^\\d{4}$", message = "계좌 비밀번호는 4자리 숫자여야 합니다.")
	private String accountPassword;
}
