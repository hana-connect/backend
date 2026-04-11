package com.hanaro.hanaconnect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.service.MemberServiceImpl;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "유저 관련", description = "내 지갑 잔액 조회") // 추가로 api에 대한 설명 description 작성해주세용~
public class MemberController {
	private final MemberServiceImpl memberService;

	// 내 지갑(가상계좌) 잔액 조회
	// 아이디를 따로 받지 않으므로 JWT 안에 들어있는 정보로만 사용자 식별 가능
	// JWT로 사용자 확인하기 위해 AuthenticationPrincipal 사용
	@GetMapping("/wallet")
	public ResponseEntity<CustomAPIResponse<?>> getMyWallet(
		@AuthenticationPrincipal TokenMemberPrincipal principal
	) {
		// 사용자 없는 경우는 Spring Security의 Filter Chain에서 막히므로 여기서 작성할 필요 X
		return memberService.getMyWallet(principal.getMemberId());
	}
}
