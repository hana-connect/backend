package com.hanaro.hanaconnect.common.security;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenMemberPrincipal {
	private Long memberId;
	private String name;
	private String virtualAccount;
	private MemberRole memberRole;
	private Role role;
}
