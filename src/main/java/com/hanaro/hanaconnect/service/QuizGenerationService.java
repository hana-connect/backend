package com.hanaro.hanaconnect.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.ai.QuizAiClient;
import com.hanaro.hanaconnect.dto.QuizGenerationResponseDTO;
import com.hanaro.hanaconnect.entity.Mission;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.entity.QuizSet;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.QuizSetRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional
public class QuizGenerationService {

	private final MissionRepository missionRepository;
	private final QuizSetRepository quizSetRepository;
	private final QuizAiClient quizAiClient;
	private final ObjectMapper objectMapper;

	public QuizSet generateTodayQuiz(Long kidId, LocalDate quizDate) {

		// 퀴즈 세트 있는 경우, 오늘 퀴즈가 이미 있으면 반환
		QuizSet existingQuizSet = quizSetRepository.findByChildIdAndQuizDate(kidId, quizDate)
			.orElse(null);

		if (existingQuizSet != null) {
			return existingQuizSet;
		}

		// 1. mission 조회
		List<Mission> missions = missionRepository.findTop10ByKidIdAndIsCompletedTrueOrderByIdDesc(kidId);

		// mission 없으면 예외
		if (missions.isEmpty()) {
			throw new IllegalArgumentException("퀴즈 생성을 위한 활동 기록이 없습니다.");
		}

		// 2. AI 입력용 mission 이름 정리
		List<String> missionNames = missions.stream()
			.map(Mission::getName)
			.distinct()
			.toList();

		// 프롬프트에 넣기 좋게 문자열로 만들기
		String missionList = missionNames.stream()
			.map(name -> "- " + name)
			.collect(Collectors.joining("\n"));

		// 3. 프롬프트 생성
		String prompt = createPrompt(missionList);

		try {
			// 4. AI 호출
			String responseJson = quizAiClient.generateQuizFromAi(prompt);

			// 5. JSON → DTO 파싱
			QuizGenerationResponseDTO response =
				objectMapper.readValue(responseJson, QuizGenerationResponseDTO.class);

			// 6. 응답 검증
			validateResponse(response);

			// 7. QuizSet 생성
			QuizSet quizSet = QuizSet.create(kidId, quizDate, 3);

			// 8. QuizQuestion 생성 후 연결
			for (QuizGenerationResponseDTO.QuestionItem item : response.getQuestions()) {
				QuizQuestion question = QuizQuestion.create(
					item.getQuestionOrder(),
					item.getQuestion(),
					item.getChoices(),
					item.getCorrectIndex(),
					item.getHint()
				);

				quizSet.addQuestion(question);
			}

			// 9. 저장
			try {
				return quizSetRepository.save(quizSet);
			} catch (DataIntegrityViolationException e) {
				return quizSetRepository.findByChildIdAndQuizDate(kidId, quizDate)
					.orElseThrow(() -> new RuntimeException("이미 생성된 퀴즈 조회에 실패했습니다.", e));
			}

		} catch (Exception e) {
			throw new RuntimeException("AI 퀴즈 생성에 실패했습니다.", e);
		}
	}

	// 프롬프트
	private String createPrompt(String missionList) {
		return """
	당신은 아이의 실제 활동 기록을 기반으로 부모나 조부모가 맞힐 수 있는 퀴즈를 생성하는 AI입니다.

	아래는 한 아이가 최근에 완료한 활동 목록입니다:
	%s

	이 활동 목록을 기반으로 객관식 퀴즈 3개를 생성하세요.

	========================
	[핵심 목적]
	========================
	퀴즈는 부모/조부모가 아이의 실제 활동 기록을 기억하고 있는지 맞히는 용도입니다.
	따라서 문제는 반드시 "어떤 활동을 했는지 / 하지 않았는지"를 구분하는 방식이어야 합니다.

	========================
	[문제 유형 - 반드시 이 2가지 중 하나만 사용]
	========================
	1. 아이가 실제로 수행한 활동 1개를 고르는 문제
	2. 아이가 수행하지 않은 활동 1개를 고르는 문제

	========================
	[보기 구성 규칙 - 매우 중요]
	========================
	1. "수행한 활동을 고르는 문제"의 경우:
	   - 보기 4개 중 정답 1개는 실제 활동 목록에 포함된 활동
	   - 나머지 오답 3개는 실제 활동 목록에 없는 활동

	2. "수행하지 않은 활동을 고르는 문제"의 경우:
	   - 보기 4개 중 정답 1개는 실제 활동 목록에 없는 활동
	   - 나머지 오답 3개는 실제 활동 목록에 포함된 활동

	3. 모든 선택지는 반드시 "구체적인 활동 이름" 형태여야 합니다.
	4. "네, 아니요, 잘 모르겠어요, 아마 ~" 같은 선택지는 절대 사용하지 마세요.
	5. 반드시 정답이 하나만 존재해야 합니다.

	========================
	[절대 금지]
	========================
	1. 활동의 의미를 해석하지 마세요.
	   (예: 창의력, 문제 해결 능력, 자연학습 등 금지)
	2. 추측형 질문 금지
	   (예: "~했을까요?" 금지)
	3. OX형 질문 금지
	4. 감정, 성격, 능력 해석 금지
	5. 활동 이름이 아닌 문장형 선택지 금지

	========================
	[스타일]
	========================
	1. 문제는 부모/조부모가 아이의 활동을 떠올리며 편하게 맞힐 수 있도록 자연스럽고 부드러운 말투로 작성하세요.
	2. 너무 딱딱한 시험 문제 말투는 피하세요.
	3. 예를 들어 "다음 중 ~은 무엇인가요?"만 반복하지 말고,
	   "우리 아이가 이번에 해본 활동은 무엇일까요?",
	   "아래 보기 중 아이가 실제로 한 활동을 골라주세요.",
	   "이번 기록에 없었던 활동은 무엇일까요?"
	   처럼 조금 더 자연스럽게 표현하세요.
	4. 다만 질문은 반드시 명확해야 하며, 추측을 유도하면 안 됩니다.
	5. 너무 유치하거나 과한 감탄 표현은 사용하지 마세요.
	6. 같은 문장 구조를 반복하지 마세요.

	========================
	[힌트 규칙]
	========================
	1. 힌트는 정답 활동 이름을 직접 말하면 안 됩니다.
	2. 힌트는 너무 추상적이면 안 됩니다.
	3. 힌트는 "기록을 떠올리면 알 수 있는 정도"로만 작성하세요.

	========================
	[출력 규칙]
	========================
	1. 반드시 서로 다른 문제 3개를 생성하세요.
	2. correctIndex는 0~3 사이 값이어야 합니다.
	3. 반드시 JSON 형식으로만 응답하고 다른 설명은 절대 포함하지 마세요.

	형식:

	{
	  "questions": [
	    {
	      "questionOrder": 1,
	      "question": "문제 내용",
	      "choices": ["보기1", "보기2", "보기3", "보기4"],
	      "correctIndex": 0,
	      "hint": "힌트"
	    }
	  ]
	}

	좋은 문제 예시:
	- 다음 중 아이가 실제로 한 활동은 무엇일까요?
	- 아래 활동 중 우리 아이가 하지 않은 것은 무엇일까요?
	- 다음 보기 중 활동 기록에 포함된 것은 무엇인가요?
	- 다음 중 활동 목록에 없는 것은 무엇일까요?
	""".formatted(missionList);
	}

	private void validateResponse(QuizGenerationResponseDTO response) {
		if (response == null || response.getQuestions() == null || response.getQuestions().size() != 3) {
			throw new IllegalArgumentException("AI가 3개의 문제를 생성하지 않았습니다.");
		}

		Set<Integer> questionOrders = new HashSet<>();

		for (QuizGenerationResponseDTO.QuestionItem item : response.getQuestions()) {
			if (item.getQuestionOrder() == null || item.getQuestionOrder() < 1 || item.getQuestionOrder() > 3) {
				throw new IllegalArgumentException("문항 순서가 올바르지 않습니다.");
			}

			if (!questionOrders.add(item.getQuestionOrder())) {
				throw new IllegalArgumentException("문항 순서가 중복되었습니다.");
			}

			if (item.getChoices() == null || item.getChoices().size() != 4) {
				throw new IllegalArgumentException("선택지 개수가 올바르지 않습니다.");
			}

			if (item.getCorrectIndex() == null || item.getCorrectIndex() < 0 || item.getCorrectIndex() > 3) {
				throw new IllegalArgumentException("정답 인덱스가 올바르지 않습니다.");
			}
		}
	}
}
