package com.hanaro.hanaconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.QuizEntryResponseDTO;
import com.hanaro.hanaconnect.service.QuizService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api") // TODO 나중에 quiz로 통일
@RequiredArgsConstructor
@Tag(name = "퀴즈", description = "아이 퀴즈")
public class QuizController {

	private final QuizService quizService;

	// 부모가 선택한 아이의 오늘 퀴즈 조회
	@GetMapping("/quiz/today")
	public ResponseEntity<CustomAPIResponse<QuizEntryResponseDTO>> enterTodayQuiz(
		@AuthenticationPrincipal TokenMemberPrincipal principal,
		@RequestParam Long childId
	) {
		QuizEntryResponseDTO response =
			quizService.enterTodayQuiz(principal.getMemberId(), childId);

		return ResponseEntity.ok(
			CustomAPIResponse.createSuccess(
				HttpStatus.OK.value(),
				response,
				"오늘의 퀴즈 조회에 성공했습니다."
			)
		);
	}

}
