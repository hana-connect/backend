package com.hanaro.hanaconnect.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class QuizGenerationResponseDTO {

	private List<QuestionItem> questions;

	// 문제 1개 단위
	@Getter
	public static class QuestionItem {

		private Integer questionOrder;
		private String question;
		private List<String> choices;
		private Integer correctIndex;
		private String hint;
	}
}
