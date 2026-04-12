package com.hanaro.hanaconnect.dto;

import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizAnswerResponseDTO {

	private Integer questionOrder; // 현재 제출한 믄제 순서(1,2,3)

	private Boolean isCorrect; // 정답 여부

	private QuizQuestionStatus questionStatus; // 문제 상태

	private Integer selectedIndex; // 정답 선택 인덱스

	private Integer correctIndex; // 실제 정답 인덱스

	private String correctAnswer; // 실제 정답 내용

	private Integer solvedCount; // 현패까지 푼 문제 개수

	private QuizSetStatus quizSetStatus; // 퀴즈 전체 상태

	private Boolean hasNextQuestion; // 다음 문제가 존재하는지 여부
}
