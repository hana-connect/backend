package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;
import com.hanaro.hanaconnect.dto.QuizAnswerResponseDTO;
import com.hanaro.hanaconnect.dto.QuizEntryResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;

// @Disabled("로컬 환경에서만 OpenAI 실제 호출 테스트 실행")
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class QuizServiceTest {

	@Autowired
	private QuizService quizService;

	@Autowired
	private QuizQuestionRepository quizQuestionRepository;

	@Autowired
	private MemberRepository memberRepository;

	private Member findKid() {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals("홍길동"))
			.filter(member -> member.getMemberRole() == MemberRole.KID)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 아이 회원을 찾을 수 없습니다."));
	}

	private Member findParent() {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals("김엄마"))
			.filter(member -> member.getMemberRole() == MemberRole.PARENT)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 부모 회원을 찾을 수 없습니다."));
	}

	// AI 퀴즈 생성 테스트
	@Test
	void aiQuizGenerationTest() {
		// InitLoader 기준
		// ID가 고정값이 아니어서 find로 찾아와야 함
		memberRepository.findAll().forEach(m ->
			System.out.println(m.getId() + " / " + m.getName() + " / " + m.getMemberRole())
		);
		Member parent = findParent();
		Member child = findKid();

		QuizEntryResponseDTO response = quizService.enterTodayQuiz(parent.getId(), child.getId());

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
		Member parent = findParent();
		Member child = findKid();

		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), child.getId());
		Long quizSetId = entry.getQuizSetId();
		Integer questionOrder = 1;

		QuizQuestion question = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(quizSetId, questionOrder)
			.orElseThrow();

		Integer correctIndex = question.getCorrectIndex();

		QuizAnswerResponseDTO response = quizService.submitAnswer(
			parent.getId(),
			child.getId(),
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
		Member parent = findParent();
		Member child = findKid();

		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), child.getId());
		Long quizSetId = entry.getQuizSetId();
		Integer questionOrder = 1;

		QuizQuestion question = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(quizSetId, questionOrder)
			.orElseThrow();

		Integer wrongIndex = (question.getCorrectIndex() + 1) % 4;

		QuizAnswerResponseDTO response = quizService.submitAnswer(
			parent.getId(),
			child.getId(),
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
