package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.dto.quiz.QuizAnswerResponseDTO;
import com.hanaro.hanaconnect.dto.quiz.QuizEntryResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Mission;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;
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
	private MemberRepository memberRepository;

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private MissionRepository missionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private static long virtualAccountSeq = 40000000000L;

	@MockitoBean
	private QuizAiClient quizAiClient;

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
				.virtualAccount(rawVirtualAccount)
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

	@Test
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

	private void createSampleMissions(Member kid, Member parent) {
		missionRepository.saveAll(List.of(
			Mission.builder().kid(kid).parent(parent).name("부모님께 인사하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("심부름 다녀오기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("용돈 기록 작성하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("방 정리하기").isCompleted(true).build(),
			Mission.builder().kid(kid).parent(parent).name("오늘 소비 내역 확인하기").isCompleted(true).build()
		));
	}
}
