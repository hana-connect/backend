package com.hanaro.hanaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {
	private String accessToken;
	private String tokenType;
	private Long memberId;
	private String name;
	private String role;
	private String memberRole;
}
