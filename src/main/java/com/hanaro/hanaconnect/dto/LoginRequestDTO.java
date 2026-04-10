package com.hanaro.hanaconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {

	@NotNull(message = "memberId는 필수입니다.")
	private Long memberId;

	@NotBlank(message = "간편비밀번호는 필수입니다.")
	@Pattern(regexp = "^\\d{6}$", message = "간편비밀번호는 6자리 숫자여야 합니다.")
	private String password;
}
