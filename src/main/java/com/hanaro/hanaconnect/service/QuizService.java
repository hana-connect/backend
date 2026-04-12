package com.hanaro.hanaconnect.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;
import com.hanaro.hanaconnect.dto.QuizEntryResponseDTO;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.entity.QuizSet;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;
import com.hanaro.hanaconnect.repository.QuizSetRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

	private final QuizSetRepository quizSetRepository;
	private final QuizQuestionRepository quizQuestionRepository;
	private final QuizGenerationService quizGenerationService;
	private final RelationRepository relationRepository;
	/*
	 * 오늘 퀴즈 조회
	 * - 있으면 반환
	 * - 없으면 생성
	 */
	@Transactional
	public QuizSet getOrCreateTodayQuiz(Long childId) {
		LocalDate today = LocalDate.now();

		return quizSetRepository.findByChildIdAndQuizDate(childId, today)
			.orElseGet(() -> createTodayQuiz(childId, today));
	}

	/*
	 * 오늘 퀴즈 생성
	 */
	@Transactional
	public QuizSet createTodayQuiz(Long childId, LocalDate quizDate) {
		return quizGenerationService.generateTodayQuiz(childId, quizDate);
	}

	/*
	 * 퀴즈 입장 + 문제 조회
	 * - 오늘 퀴즈를 조회하거나 없으면 생성
	 * - 프론트에 내려줄 응답 DTO로 변환
	 */
	@Transactional
	public QuizEntryResponseDTO enterTodayQuiz(Long parentId, Long childId) {
		validateParentChildRelation(parentId, childId);

		QuizSet quizSet = getOrCreateTodayQuiz(childId);

		List<QuizEntryResponseDTO.QuestionItem> questions = quizSet.getQuestions().stream()
			.sorted(Comparator.comparing(QuizQuestion::getQuestionOrder))
			.map(question -> QuizEntryResponseDTO.QuestionItem.builder()
				.questionOrder(question.getQuestionOrder())
				.question(question.getQuestion())
				.choices(question.getChoices())
				.hint(question.getHint())
				.status(question.getStatus())
				.selectedIndex(question.getSelectedIndex())
				.build())
			.toList();

		return QuizEntryResponseDTO.builder()
			.quizSetId(quizSet.getId())
			.quizDate(quizSet.getQuizDate())
			.status(quizSet.getStatus())
			.solvedCount(quizSet.getSolvedCount())
			.totalCount(quizSet.getTotalCount())
			.questions(questions)
			.build();
	}

	// 관계 검증
	private void validateParentChildRelation(Long parentId, Long childId) {
		boolean exists = relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(parentId, childId, MemberRole.KID);

		if (!exists) {
			throw new IllegalArgumentException("해당 아이에 대한 접근 권한이 없습니다.");
		}
	}

	/*
	 * 현재 풀어야 할 문제 조회
	 * - READY 상태 문제 중 questionOrder가 가장 작은 문제 반환
	 */
	public QuizQuestion getCurrentQuestion(Long quizSetId) {
		List<QuizQuestion> questions = quizQuestionRepository.findByQuizSetIdOrderByQuestionOrderAsc(quizSetId);

		return questions.stream()
			.filter(question -> question.getStatus() == QuizQuestionStatus.READY)
			.findFirst()
			.orElse(null);
	}

	/*
	 * 답 제출
	 * - 아직 처리되지 않은(READY) 문제만 처리
	 * - question 상태 변경
	 * - quizSet solvedCount 증가
	 */
	@Transactional
	public void submitAnswer(Long quizSetId, Integer questionOrder, Integer selectedIndex) {
		QuizSet quizSet = quizSetRepository.findById(quizSetId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀴즈 세트입니다."));

		QuizQuestion question = quizQuestionRepository.findByQuizSetIdAndQuestionOrder(quizSetId, questionOrder)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다."));

		if (question.getStatus() == QuizQuestionStatus.READY) {
			question.submitAnswer(selectedIndex);
			quizSet.increaseSolvedCount();
		}
	}

	/*
	 * 문제 이탈 처리
	 * - 아직 처리되지 않은(READY) 문제만 WRONG 처리
	 * - solvedCount 증가
	 */
	@Transactional
	public void abandonQuestion(Long quizSetId, Integer questionOrder) {
		QuizSet quizSet = quizSetRepository.findById(quizSetId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀴즈 세트입니다."));

		QuizQuestion question = quizQuestionRepository.findByQuizSetIdAndQuestionOrder(quizSetId, questionOrder)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다."));

		if (question.getStatus() == QuizQuestionStatus.READY) {
			question.markWrong();
			quizSet.increaseSolvedCount();
		}
	}

	/*
	 * 완료 여부 확인
	 */
	public boolean isCompleted(Long quizSetId) {
		QuizSet quizSet = quizSetRepository.findById(quizSetId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀴즈 세트입니다."));

		return quizSet.getStatus() == QuizSetStatus.COMPLETED;
	}
}
