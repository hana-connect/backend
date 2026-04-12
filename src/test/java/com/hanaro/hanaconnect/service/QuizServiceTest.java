package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.dto.QuizEntryResponseDTO;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.entity.QuizSet;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;

@Disabled("로컬 환경에서만 OpenAI 실제 호출 테스트 실행")
@SpringBootTest
class QuizServiceTest {

	@Autowired
	private QuizService quizService;
	
	// AI 퀴즈 생성 테스트
	@Test
	void aiQuizGenerationTest() {
		// InitLoader 기준
		// parent id = 1, kid id = 2
		Long parentId = 1L;
		Long childId = 2L;

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
}
