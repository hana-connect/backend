package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;
import com.hanaro.hanaconnect.dto.QuizAnswerResponseDTO;
import com.hanaro.hanaconnect.dto.QuizEntryResponseDTO;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;

@Disabled("로컬 환경에서만 OpenAI 실제 호출 테스트 실행")
@SpringBootTest
@Transactional
class QuizServiceTest {

	@Autowired
	private QuizService quizService;

	@Autowired
	private QuizQuestionRepository quizQuestionRepository;

	// AI 퀴즈 생성 테스트
	@Test
	void aiQuizGenerationTest() {
		// InitLoader 기준
		// kid1 = 1, parent1 = 2
		Long parentId = 2L;
		Long childId = 1L;

		QuizEntryResponseDTO response = quizService.enterTodayQuiz(parentId, childId);

		assertThat(response).isNotNull();
		assertThat(response.getQuizSetId()).isNotNull();
		assertThat(response.getQuestions()).hasSize(3);

		for (QuizEntryResponseDTO.QuestionItem question : response.getQuestions()) {
			assertThat(question.getQuestion()).isNotBlank();
			assertThat(question.getChoices()).hasSize(4);
			assertThat(question.getStatus()).isEqualTo(QuizQuestionStatus.READY);
			assertThat(question.getSelectedIndex()).isNull();
		}
	}

	@Test
	void submitAnswer_correctAnswerTest() {
		Long parentId = 2L;
		Long childId = 1L;

		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parentId, childId);
		Long quizSetId = entry.getQuizSetId();
		Integer questionOrder = 1;

		QuizQuestion question = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(quizSetId, questionOrder)
			.orElseThrow();

		Integer correctIndex = question.getCorrectIndex();

		QuizAnswerResponseDTO response = quizService.submitAnswer(
			parentId,
			childId,
			quizSetId,
			questionOrder,
			correctIndex
		);

		assertThat(response).isNotNull();
		assertThat(response.getQuestionOrder()).isEqualTo(questionOrder);
		assertThat(response.getIsCorrect()).isTrue();
		assertThat(response.getQuestionStatus()).isEqualTo(QuizQuestionStatus.CORRECT);
		assertThat(response.getSelectedIndex()).isEqualTo(correctIndex);
		assertThat(response.getCorrectIndex()).isEqualTo(correctIndex);
		assertThat(response.getCorrectAnswer()).isEqualTo(question.getCorrectAnswer());
		assertThat(response.getSolvedCount()).isEqualTo(1);
		assertThat(response.getQuizSetStatus()).isEqualTo(QuizSetStatus.IN_PROGRESS);
		assertThat(response.getHasNextQuestion()).isTrue();
	}

	@Test
	void submitAnswer_wrongAnswerTest() {
		Long parentId = 2L;
		Long childId = 1L;

		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parentId, childId);
		Long quizSetId = entry.getQuizSetId();
		Integer questionOrder = 1;

		QuizQuestion question = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(quizSetId, questionOrder)
			.orElseThrow();

		Integer wrongIndex = (question.getCorrectIndex() + 1) % 4;

		QuizAnswerResponseDTO response = quizService.submitAnswer(
			parentId,
			childId,
			quizSetId,
			questionOrder,
			wrongIndex
		);

		assertThat(response).isNotNull();
		assertThat(response.getQuestionOrder()).isEqualTo(questionOrder);
		assertThat(response.getIsCorrect()).isFalse();
		assertThat(response.getQuestionStatus()).isEqualTo(QuizQuestionStatus.WRONG);
		assertThat(response.getSelectedIndex()).isEqualTo(wrongIndex);
		assertThat(response.getCorrectIndex()).isEqualTo(question.getCorrectIndex());
		assertThat(response.getCorrectAnswer()).isEqualTo(question.getCorrectAnswer());
		assertThat(response.getSolvedCount()).isEqualTo(1);
		assertThat(response.getQuizSetStatus()).isEqualTo(QuizSetStatus.IN_PROGRESS);
		assertThat(response.getHasNextQuestion()).isTrue();
	}
}
