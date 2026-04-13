package com.hanaro.hanaconnect.dto;

import com.hanaro.hanaconnect.common.validator.AccountNumber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "아이 계좌 추가 요청")
public class KidAccountAddRequestDTO {

	@Schema(
		description = "계좌번호 11자리 숫자",
		example = "11122223333"
	)
	@NotBlank(message = "계좌번호는 필수입니다.")
	@AccountNumber
	private String accountNumber;

	@Schema(
		description = "부모/조부모가 아이 계좌를 구분하기 위한 별명",
		example = "민수 청약통장"
	)
	@NotBlank(message = "계좌 별명은 필수입니다.")
	@Size(max = 50, message = "계좌 별명은 50자 이하여야 합니다.")
	private String nickname;
}
