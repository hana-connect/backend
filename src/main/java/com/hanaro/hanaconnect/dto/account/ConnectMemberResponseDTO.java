package com.hanaro.hanaconnect.dto.account;

import com.hanaro.hanaconnect.common.enums.MemberRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ConnectMemberResponseDTO {
	private Long connectMemberId;			// 연결된 멤버의 pk
	private String connectMemberName;      // 연결된 멤버의 실제 이름
	private String connectMemberPhoneName; // 로그인 유저가 연결된 유저를 전화번호에 저장한 이름
	private MemberRole connectMemberRole; // 연결된 유저의 부모/아이 회원 역할
}
