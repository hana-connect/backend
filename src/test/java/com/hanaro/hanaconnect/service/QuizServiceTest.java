package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.entity.QuizSet;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;
import com.hanaro.hanaconnect.repository.QuizSetRepository;

@SpringBootTest
class QuizServiceTest {

	@Autowired
	private QuizService quizService;

	@Autowired
	private QuizSetRepository quizSetRepository;

	@Autowired
	private QuizQuestionRepository quizQuestionRepository;

	// 퀴즈 생성 테스트
	@Test
	void createTodayQuizTest() {
		quizService.createTodayQuiz(1L, LocalDate.now());
	}

	// 정답 제출 테스트
	@Test
	void submitCorrectAnswerTest() {
		Long childId = 2L;
		LocalDate today = LocalDate.now();

		QuizSet quizSet = quizService.createTodayQuiz(childId, today);

		quizService.submitAnswer(quizSet.getId(), 1, 2);

		QuizSet savedQuizSet = quizSetRepository.findById(quizSet.getId())
			.orElseThrow();

		QuizQuestion question = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(quizSet.getId(), 1)
			.orElseThrow();

		assertThat(question.getSelectedIndex()).isEqualTo(2);
		assertThat(question.getStatus()).isEqualTo(QuizQuestionStatus.CORRECT);

		assertThat(savedQuizSet.getSolvedCount()).isEqualTo(1);
		assertThat(savedQuizSet.getStatus()).isEqualTo(QuizSetStatus.IN_PROGRESS);
	}

	// 오답 제출 테스트
	@Test
	void submitWrongAnswerTest() {
		Long childId = 3L;
		LocalDate today = LocalDate.now();

		QuizSet quizSet = quizService.createTodayQuiz(childId, today);

		// 1번 문제의 정답은 2이므로, 일부러 오답 1 제출
		quizService.submitAnswer(quizSet.getId(), 1, 1);

		QuizSet savedQuizSet = quizSetRepository.findById(quizSet.getId())
			.orElseThrow();

		QuizQuestion question = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(quizSet.getId(), 1)
			.orElseThrow();

		assertThat(question.getSelectedIndex()).isEqualTo(1);
		assertThat(question.getStatus()).isEqualTo(QuizQuestionStatus.WRONG);

		assertThat(savedQuizSet.getSolvedCount()).isEqualTo(1);
		assertThat(savedQuizSet.getStatus()).isEqualTo(QuizSetStatus.IN_PROGRESS);
	}

	// 퀴즈 완료 테스트
	@Test
	void completeQuizTest() {
		Long childId = 4L;
		LocalDate today = LocalDate.now();

		QuizSet quizSet = quizService.createTodayQuiz(childId, today);

		quizService.submitAnswer(quizSet.getId(), 1, 2);
		quizService.submitAnswer(quizSet.getId(), 2, 1);
		quizService.submitAnswer(quizSet.getId(), 3, 0);

		QuizSet savedQuizSet = quizSetRepository.findById(quizSet.getId())
			.orElseThrow();

		List<QuizQuestion> questions = quizQuestionRepository
			.findByQuizSetIdOrderByQuestionOrderAsc(quizSet.getId());

		assertThat(savedQuizSet.getSolvedCount()).isEqualTo(3);
		assertThat(savedQuizSet.getStatus()).isEqualTo(QuizSetStatus.COMPLETED);

		assertThat(questions).hasSize(3);
		assertThat(questions.get(0).getStatus()).isEqualTo(QuizQuestionStatus.CORRECT);
		assertThat(questions.get(1).getStatus()).isEqualTo(QuizQuestionStatus.CORRECT);
		assertThat(questions.get(2).getStatus()).isEqualTo(QuizQuestionStatus.CORRECT);
	}

	// 문제 이탈 테스트
	@Test
	void abandonQuestionTest() {
		Long childId = 5L;
		LocalDate today = LocalDate.now();

		QuizSet quizSet = quizService.createTodayQuiz(childId, today);

		// 2번 문제 진입 후 이탈했다고 가정
		quizService.abandonQuestion(quizSet.getId(), 2);

		QuizSet savedQuizSet = quizSetRepository.findById(quizSet.getId())
			.orElseThrow();

		QuizQuestion question = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(quizSet.getId(), 2)
			.orElseThrow();

		assertThat(question.getStatus()).isEqualTo(QuizQuestionStatus.WRONG);
		assertThat(savedQuizSet.getSolvedCount()).isEqualTo(1);
		assertThat(savedQuizSet.getStatus()).isEqualTo(QuizSetStatus.IN_PROGRESS);
	}
}
