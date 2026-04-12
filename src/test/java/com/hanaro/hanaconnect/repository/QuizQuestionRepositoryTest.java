package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.entity.QuizSet;

@Disabled("로컬 환경에서만 테스트 실행")
class QuizQuestionRepositoryTest extends BaseRepositoryTest {

	@Autowired
	private QuizQuestionRepository quizQuestionRepository;

	@Autowired
	private QuizSetRepository quizSetRepository;

	@Test
	void findByQuizSetIdOrderByQuestionOrderAsc() {
		// given
		QuizSet quizSet = QuizSet.create(1L, LocalDate.now(), 3);
		quizSetRepository.save(quizSet);

		QuizQuestion q1 = QuizQuestion.builder()
			.quizSet(quizSet)
			.questionOrder(2)
			.question("문제2")
			.choices(List.of("A", "B", "C", "D"))
			.correctIndex(0)
			.selectedIndex(null)
			.status(QuizQuestionStatus.READY)
			.hint("힌트2")
			.build();

		QuizQuestion q2 = QuizQuestion.builder()
			.quizSet(quizSet)
			.questionOrder(1)
			.question("문제1")
			.choices(List.of("A", "B", "C", "D"))
			.correctIndex(1)
			.selectedIndex(null)
			.status(QuizQuestionStatus.READY)
			.hint("힌트1")
			.build();

		quizQuestionRepository.save(q1);
		quizQuestionRepository.save(q2);

		// when
		List<QuizQuestion> result =
			quizQuestionRepository.findByQuizSetIdOrderByQuestionOrderAsc(quizSet.getId());

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getQuestionOrder()).isEqualTo(1);
		assertThat(result.get(1).getQuestionOrder()).isEqualTo(2);
	}

	@Test
	void findByQuizSetIdAndQuestionOrder() {
		// given
		QuizSet quizSet = QuizSet.create(1L, LocalDate.now(), 3);
		quizSetRepository.save(quizSet);

		QuizQuestion question = QuizQuestion.builder()
			.quizSet(quizSet)
			.questionOrder(1)
			.question("문제1")
			.choices(List.of("A", "B", "C", "D"))
			.correctIndex(1)
			.selectedIndex(null)
			.status(QuizQuestionStatus.READY)
			.hint("힌트1")
			.build();

		quizQuestionRepository.save(question);

		// when
		Optional<QuizQuestion> result =
			quizQuestionRepository.findByQuizSetIdAndQuestionOrder(quizSet.getId(), 1);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().getQuestion()).isEqualTo("문제1");
		assertThat(result.get().getQuestionOrder()).isEqualTo(1);
	}
}
