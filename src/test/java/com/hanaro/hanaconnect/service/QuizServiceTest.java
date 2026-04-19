package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.ai.QuizAiClient;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.quiz.QuizAnswerResponseDTO;
import com.hanaro.hanaconnect.dto.quiz.QuizEntryResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Mission;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.entity.QuizSet;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;
import com.hanaro.hanaconnect.repository.QuizSetRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class QuizServiceTest {

	@Autowired
	private QuizService quizService;

	@Autowired
	private QuizQuestionRepository quizQuestionRepository;

	@Autowired
	private QuizSetRepository quizSetRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private MissionRepository missionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccountCryptoService accountCryptoService;

	@MockitoBean
	private QuizAiClient quizAiClient;

	private static long virtualAccountSeq = 40000000000L;

	private Member kid;
	private Member parent;

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

		kid = createMember("홍길동", MemberRole.KID);
		parent = createMember("김엄마", MemberRole.PARENT);

		relationRepository.save(createRelation(kid, parent));
		relationRepository.save(createRelation(parent, kid));

		createSampleMissions(kid, parent);
	}

	private Member createMember(String name, MemberRole memberRole) {
		String rawVirtualAccount = generateVirtualAccount();

		return memberRepository.save(
			Member.builder()
				.name(name)
				.virtualAccount(accountCryptoService.encrypt(rawVirtualAccount))
				.birthday(memberRole == MemberRole.KID
					? LocalDate.of(2015, 1, 1)
					: LocalDate.of(1985, 1, 1))
				.password(passwordEncoder.encode("123456"))
				.memberRole(memberRole)
				.role(Role.USER)
				.build()
		);
	}

	private String generateVirtualAccount() {
		return String.valueOf(virtualAccountSeq++);
	}

	private Relation createRelation(Member member, Member connectMember) {
		return Relation.builder()
			.member(member)
			.connectMember(connectMember)
			.connectMemberRole(connectMember.getMemberRole())
			.build();
	}

	private void createSampleMissions(Member kid, Member parent) {
		missionRepository.saveAll(List.of(
			Mission.builder().kid(kid).parent(parent).name("부모님께 인사하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("심부름 다녀오기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("용돈 기록 작성하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("방 정리하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("오늘 소비 내역 확인하기").isCompleted(true).build()
		));
	}

	@Test
	@DisplayName("퀴즈 입장 시 오늘 퀴즈를 생성하고 문제를 반환한다")
	void aiQuizGenerationTest() {
		QuizEntryResponseDTO response = quizService.enterTodayQuiz(parent.getId(), kid.getId());

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
	@DisplayName("같은 날 다시 입장하면 기존 퀴즈를 재사용한다")
	void enterTodayQuiz_reuseTodayQuiz() {
		QuizEntryResponseDTO first = quizService.enterTodayQuiz(parent.getId(), kid.getId());
		QuizEntryResponseDTO second = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		assertThat(first.getQuizSetId()).isEqualTo(second.getQuizSetId());
		assertThat(second.getQuestions()).hasSize(3);
	}

	@Test
	@DisplayName("부모-아이 관계가 없으면 퀴즈 입장에 실패한다")
	void enterTodayQuiz_noRelation_throwsException() {
		Member strangerParent = createMember("낯선부모", MemberRole.PARENT);

		assertThatThrownBy(() -> quizService.enterTodayQuiz(strangerParent.getId(), kid.getId()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("해당 아이에 대한 접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("정답 제출에 성공하면 상태와 풀이 수가 갱신된다")
	void submitAnswer_success() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		QuizAnswerResponseDTO response =
			quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 1, 0);

		assertThat(response.getQuestionOrder()).isEqualTo(1);
		assertThat(response.getIsCorrect()).isTrue();
		assertThat(response.getQuestionStatus()).isEqualTo(QuizQuestionStatus.CORRECT);
		assertThat(response.getSelectedIndex()).isEqualTo(0);
		assertThat(response.getCorrectIndex()).isEqualTo(0);
		assertThat(response.getCorrectAnswer()).isNotBlank();
		assertThat(response.getSolvedCount()).isEqualTo(1);
		assertThat(response.getHasNextQuestion()).isTrue();

		QuizQuestion savedQuestion = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(entry.getQuizSetId(), 1)
			.orElseThrow();

		assertThat(savedQuestion.getStatus()).isEqualTo(QuizQuestionStatus.CORRECT);
		assertThat(savedQuestion.getSelectedIndex()).isEqualTo(0);
	}

	@Test
	@DisplayName("오답 제출도 정상 처리되고 WRONG 상태가 된다")
	void submitAnswer_wrongAnswer_success() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		QuizAnswerResponseDTO response =
			quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 1, 1);

		assertThat(response.getIsCorrect()).isFalse();
		assertThat(response.getQuestionStatus()).isEqualTo(QuizQuestionStatus.WRONG);
		assertThat(response.getSelectedIndex()).isEqualTo(1);
		assertThat(response.getSolvedCount()).isEqualTo(1);

		QuizQuestion savedQuestion = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(entry.getQuizSetId(), 1)
			.orElseThrow();

		assertThat(savedQuestion.getStatus()).isEqualTo(QuizQuestionStatus.WRONG);
		assertThat(savedQuestion.getSelectedIndex()).isEqualTo(1);
	}

	@Test
	@DisplayName("마지막 문제를 제출하면 다음 문제가 없다고 반환한다")
	void submitAnswer_lastQuestion_hasNextQuestionFalse() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 1, 0);
		quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 2, 3);
		QuizAnswerResponseDTO last =
			quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 3, 0);

		assertThat(last.getSolvedCount()).isEqualTo(3);
		assertThat(last.getHasNextQuestion()).isFalse();
	}

	@Test
	@DisplayName("선택 인덱스가 0~3 범위를 벗어나면 예외가 발생한다")
	void submitAnswer_invalidSelectedIndex_throwsException() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		assertThatThrownBy(() ->
			quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 1, 5))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("선택한 답의 인덱스가 올바르지 않습니다.");
	}

	@Test
	@DisplayName("음수 인덱스로 답을 제출하면 예외가 발생한다")
	void submitAnswer_negativeSelectedIndex_throwsException() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		assertThatThrownBy(() ->
			quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 1, -1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("선택한 답의 인덱스가 올바르지 않습니다.");
	}

	@Test
	@DisplayName("이미 제출한 문제를 다시 제출하면 예외가 발생한다")
	void submitAnswer_alreadySolvedQuestion_throwsException() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 1, 0);

		assertThatThrownBy(() ->
			quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 1, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("이미 답을 제출한 문제입니다.");
	}

	@Test
	@DisplayName("존재하지 않는 퀴즈 세트에 답을 제출하면 예외가 발생한다")
	void submitAnswer_quizSetNotFound_throwsException() {
		assertThatThrownBy(() ->
			quizService.submitAnswer(parent.getId(), kid.getId(), 999999L, 1, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("존재하지 않는 퀴즈 세트입니다.");
	}

	@Test
	@DisplayName("존재하지 않는 문제 번호로 답을 제출하면 예외가 발생한다")
	void submitAnswer_questionNotFound_throwsException() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		assertThatThrownBy(() ->
			quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 99, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("존재하지 않는 문제입니다.");
	}

	@Test
	@DisplayName("다른 아이의 퀴즈에 답을 제출하면 예외가 발생한다")
	void submitAnswer_otherChildQuiz_throwsException() {
		Member otherKid = createMember("다른아이", MemberRole.KID);
		relationRepository.save(createRelation(otherKid, parent));
		relationRepository.save(createRelation(parent, otherKid));
		createSampleMissions(otherKid, parent);

		QuizEntryResponseDTO otherKidQuiz = quizService.enterTodayQuiz(parent.getId(), otherKid.getId());

		assertThatThrownBy(() ->
			quizService.submitAnswer(parent.getId(), kid.getId(), otherKidQuiz.getQuizSetId(), 1, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("해당 아이의 퀴즈가 아닙니다.");
	}

	@Test
	@DisplayName("부모-아이 관계가 없으면 답 제출에 실패한다")
	void submitAnswer_noRelation_throwsException() {
		Member strangerParent = createMember("권한없는부모", MemberRole.PARENT);
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		assertThatThrownBy(() ->
			quizService.submitAnswer(strangerParent.getId(), kid.getId(), entry.getQuizSetId(), 1, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("해당 아이에 대한 접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("READY 상태 문제를 이탈하면 WRONG 처리되고 solvedCount가 증가한다")
	void abandonQuestion_readyQuestion_marksWrong() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		quizService.abandonQuestion(entry.getQuizSetId(), 1);

		QuizQuestion question = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(entry.getQuizSetId(), 1)
			.orElseThrow();

		QuizSet quizSet = quizSetRepository.findById(entry.getQuizSetId()).orElseThrow();

		assertThat(question.getStatus()).isEqualTo(QuizQuestionStatus.WRONG);
		assertThat(quizSet.getSolvedCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("이미 제출한 문제를 이탈 처리해도 상태와 solvedCount는 변하지 않는다")
	void abandonQuestion_alreadySolvedQuestion_noChange() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		quizService.submitAnswer(parent.getId(), kid.getId(), entry.getQuizSetId(), 1, 0);
		quizService.abandonQuestion(entry.getQuizSetId(), 1);

		QuizQuestion question = quizQuestionRepository
			.findByQuizSetIdAndQuestionOrder(entry.getQuizSetId(), 1)
			.orElseThrow();

		QuizSet quizSet = quizSetRepository.findById(entry.getQuizSetId()).orElseThrow();

		assertThat(question.getStatus()).isEqualTo(QuizQuestionStatus.CORRECT);
		assertThat(quizSet.getSolvedCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("존재하지 않는 퀴즈 세트를 이탈 처리하면 예외가 발생한다")
	void abandonQuestion_quizSetNotFound_throwsException() {
		assertThatThrownBy(() -> quizService.abandonQuestion(999999L, 1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("존재하지 않는 퀴즈 세트입니다.");
	}

	@Test
	@DisplayName("존재하지 않는 문제를 이탈 처리하면 예외가 발생한다")
	void abandonQuestion_questionNotFound_throwsException() {
		QuizEntryResponseDTO entry = quizService.enterTodayQuiz(parent.getId(), kid.getId());

		assertThatThrownBy(() -> quizService.abandonQuestion(entry.getQuizSetId(), 99))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("존재하지 않는 문제입니다.");
	}
}
