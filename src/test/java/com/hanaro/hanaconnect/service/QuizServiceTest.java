package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.ai.QuizAiClient;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;
import com.hanaro.hanaconnect.dto.quiz.QuizAnswerResponseDTO;
import com.hanaro.hanaconnect.dto.quiz.QuizEntryResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;

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

	@MockitoBean
	private QuizAiClient quizAiClient;

	@BeforeEach
	void setUp() {
		given(quizAiClient.generateQuizFromAi(anyString()))
			.willReturn("""
				{
				  "questions": [
				    {
				      "questionOrder": 1,
				      "question": "이번에 아이가 실제로 해본 활동은 무엇일까요?",
				      "choices": ["방 정리하기", "축구 경기 보기", "영화관 가기", "캠핑 가기"],
				      "correctIndex": 0,
				      "hint": "아이의 공간을 정돈하는 활동이에요."
				    },
				    {
				      "questionOrder": 2,
				      "question": "다음 중 아이가 하지 않은 활동은 무엇일까요?",
				      "choices": ["책 읽기", "그림 그리기", "설거지 돕기", "스쿠버다이빙"],
				      "correctIndex": 3,
				      "hint": "일상에서 쉽게 하기 어려운 활동이에요."
				    },
				    {
				      "questionOrder": 3,
				      "question": "아래 보기 중 활동 기록에 포함된 것은 무엇인가요?",
				      "choices": ["분리수거 돕기", "비행기 조종", "암벽 등반", "번지점프"],
				      "correctIndex": 0,
				      "hint": "집에서 도와줄 수 있는 일이에요."
				    }
				  ]
				}
				""");
	}

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

	@Test
	void aiQuizGenerationTest() {
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
			.orElseThrow(() -> new IllegalArgumentException("테스트용 문제를 찾을 수 없습니다."));

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
			.orElseThrow(() -> new IllegalArgumentException("테스트용 문제를 찾을 수 없습니다."));

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
