package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.entity.QuizSet;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;

@SpringBootTest
class QuizServiceTest {

	@Autowired
	private QuizService quizService;

	@Autowired
	private QuizQuestionRepository quizQuestionRepository;

	// AI 퀴즈 생성 테스트
	@Test
	void aiQuizGenerationTest() {
		Long childId = 1L;

		QuizSet quizSet = quizService.getOrCreateTodayQuiz(childId);

		assertThat(quizSet).isNotNull();

		List<QuizQuestion> questions = quizQuestionRepository
			.findByQuizSetIdOrderByQuestionOrderAsc(quizSet.getId());

		assertThat(questions).hasSize(3);

		for (QuizQuestion question : questions) {
			assertThat(question.getQuestion()).isNotBlank();
			assertThat(question.getChoices()).hasSize(4);
			assertThat(question.getCorrectIndex()).isBetween(0, 3);
			assertThat(question.getStatus()).isEqualTo(QuizQuestionStatus.READY);
		}
	}
}
