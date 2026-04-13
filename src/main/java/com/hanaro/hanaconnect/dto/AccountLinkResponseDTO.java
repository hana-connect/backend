package com.hanaro.hanaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountLinkResponseDTO {

	private String accountNumber;
	private String linkedAt;
}
