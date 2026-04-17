package com.hanaro.hanaconnect.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.security.JwtAuthenticationFilter;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.quiz.QuizAnswerRequestDTO;
import com.hanaro.hanaconnect.dto.quiz.QuizAnswerResponseDTO;
import com.hanaro.hanaconnect.dto.quiz.QuizEntryResponseDTO;
import com.hanaro.hanaconnect.service.QuizService;

// @Disabled("로컬 환경에서만 OpenAI 실제 호출 테스트 실행")
@ActiveProfiles("test")
@WebMvcTest(
	controllers = QuizController.class,
	excludeAutoConfiguration = {SecurityAutoConfiguration.class},
	excludeFilters = {
		@ComponentScan.Filter(
			type = FilterType.ASSIGNABLE_TYPE,
			classes = JwtAuthenticationFilter.class
		)
	}
)
class QuizControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private QuizService quizService;

	@Test
	void enterTodayQuiz_success() throws Exception {
		TokenMemberPrincipal principal = new TokenMemberPrincipal(
			2L,
			"김엄마",
			"22233334444",
			MemberRole.PARENT,
			Role.USER
		);

		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(principal, null, List.of());

		QuizEntryResponseDTO.QuestionItem question = QuizEntryResponseDTO.QuestionItem.builder()
			.questionOrder(1)
			.question("이번에 아이가 실제로 해본 활동은 무엇일까요?")
			.choices(List.of("방 정리하기", "공원 산책하기", "책 읽어주기", "그림 그리기"))
			.hint("아이의 공간을 정돈하는 일이에요.")
			.status(QuizQuestionStatus.READY)
			.selectedIndex(null)
			.build();

		QuizEntryResponseDTO response = QuizEntryResponseDTO.builder()
			.quizSetId(1L)
			.quizDate(LocalDate.of(2026, 4, 13))
			.status(QuizSetStatus.NOT_STARTED)
			.solvedCount(0)
			.totalCount(3)
			.questions(List.of(question))
			.build();

		given(quizService.enterTodayQuiz(anyLong(), eq(1L))).willReturn(response);

		mockMvc.perform(
				get("/api/quiz/today")
					.param("childId", "1")
					.with(authentication(auth))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("오늘의 퀴즈 조회에 성공했습니다."));
	}

	@Test
	void submitAnswer_success() throws Exception {
		TokenMemberPrincipal principal = new TokenMemberPrincipal(
			2L,
			"김엄마",
			"22233334444",
			MemberRole.PARENT,
			Role.USER
		);

		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(principal, null, List.of());

		QuizAnswerRequestDTO request = new QuizAnswerRequestDTO();
		request.setSelectedIndex(3);

		QuizAnswerResponseDTO response = QuizAnswerResponseDTO.builder()
			.questionOrder(1)
			.isCorrect(false)
			.questionStatus(QuizQuestionStatus.WRONG)
			.selectedIndex(3)
			.correctIndex(0)
			.correctAnswer("식사 후 설거지 돕기")
			.solvedCount(1)
			.quizSetStatus(QuizSetStatus.IN_PROGRESS)
			.hasNextQuestion(true)
			.build();

		given(quizService.submitAnswer(anyLong(), eq(1L), eq(1L), eq(1), eq(3))).willReturn(response);

		mockMvc.perform(
				post("/api/quiz/1/questions/1/answer")
					.param("childId", "1")
					.with(authentication(auth))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("답안 제출이 완료되었습니다."));
	}
}
