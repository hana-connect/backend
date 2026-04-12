package com.hanaro.hanaconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.WalletResponseDTO;
import com.hanaro.hanaconnect.service.MemberService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "유저 관련", description = "내 지갑 잔액 조회") // 추가로 api에 대한 설명 description에 작성해주세용~
public class MemberController {
	private final MemberService memberService;

	// 내 지갑(가상계좌) 잔액 조회
	// 아이디를 따로 받지 않으므로 JWT 안에 들어있는 정보로만 사용자 식별 가능
	// JWT로 사용자 확인하기 위해 AuthenticationPrincipal 사용
	@GetMapping("/wallet")
	public ResponseEntity<CustomAPIResponse<WalletResponseDTO>> getMyWallet(
		@AuthenticationPrincipal TokenMemberPrincipal principal
	) {
		WalletResponseDTO walletResponseDTO = memberService.getMyWallet(principal.getMemberId());

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				walletResponseDTO,
				"내 지갑 잔액 조회에 성공했습니다."
			)
		);
	}
}
