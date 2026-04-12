package com.hanaro.hanaconnect.dto;

import java.time.LocalDate;
import java.util.List;

import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuizEntryResponseDTO {

	private Long quizSetId;
	private LocalDate quizDate;
	private QuizSetStatus status;
	private Integer solvedCount;
	private Integer totalCount;
	private List<QuestionItem> questions;

	@Getter
	@Builder
	@AllArgsConstructor
	public static class QuestionItem {

		private Integer questionOrder;
		private String question;
		private List<String> choices;
		private String hint;
		private QuizQuestionStatus status;
		private Integer selectedIndex;
	}
}
