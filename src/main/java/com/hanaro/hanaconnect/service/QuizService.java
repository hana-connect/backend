package com.hanaro.hanaconnect.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.dto.quiz.QuizAnswerResponseDTO;
import com.hanaro.hanaconnect.dto.quiz.QuizEntryResponseDTO;
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

	// 오늘 퀴즈 조회 (있으면 반환 / 없으면 생성)
	@Transactional
	public QuizSet getOrCreateTodayQuiz(Long childId) {
		LocalDate today = LocalDate.now();

		return quizSetRepository.findByChildIdAndQuizDate(childId, today)
			.orElseGet(() -> createTodayQuiz(childId, today));
	}

	// 오늘 퀴즈 생성
	@Transactional
	public QuizSet createTodayQuiz(Long childId, LocalDate quizDate) {
		return quizGenerationService.generateTodayQuiz(childId, quizDate);
	}

	// 퀴즈 입장 + 문제 조회 (오늘 퀴즈를 조회하거나 없으면 생성 , 응답 DTO로 변환)
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

	// 답 제출
	@Transactional
	public QuizAnswerResponseDTO submitAnswer(Long parentId, Long childId, Long quizSetId, Integer questionOrder, Integer selectedIndex){
		validateParentChildRelation(parentId, childId);

		QuizSet quizSet = quizSetRepository.findById(quizSetId)
			.orElseThrow(()-> new IllegalArgumentException("존재하지 않는 퀴즈 세트입니다."));

		if (!quizSet.getChildId().equals(childId)){
			throw new IllegalArgumentException("해당 아이의 퀴즈가 아닙니다.");
		}

		QuizQuestion question = quizQuestionRepository.findByQuizSetIdAndQuestionOrder(quizSetId, questionOrder)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다."));

		if (question.getStatus() != QuizQuestionStatus.READY) {
			throw new IllegalArgumentException("이미 답을 제출한 문제입니다.");
		}

		if (selectedIndex < 0 || selectedIndex > 3) {
			throw new IllegalArgumentException("선택한 답의 인덱스가 올바르지 않습니다.");
		}

		boolean isCorrect = question.getCorrectIndex().equals(selectedIndex);

		question.submitAnswer(selectedIndex);
		quizSet.increaseSolvedCount();

		String correctAnswer = question.getCorrectAnswer();
		boolean hasNextQuestion = !quizSet.getSolvedCount().equals(quizSet.getTotalCount());

		return QuizAnswerResponseDTO.builder()
			.questionOrder(question.getQuestionOrder())
			.isCorrect(isCorrect)
			.questionStatus(question.getStatus())
			.selectedIndex(question.getSelectedIndex())
			.correctIndex(question.getCorrectIndex())
			.correctAnswer(correctAnswer)
			.solvedCount(quizSet.getSolvedCount())
			.quizSetStatus(quizSet.getStatus())
			.hasNextQuestion(hasNextQuestion)
			.build();
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

	// 부모-아이 관계 검증
	private void validateParentChildRelation(Long parentId, Long childId) {
		boolean exists = relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(parentId, childId, MemberRole.KID);

		if (!exists) {
			throw new IllegalArgumentException("해당 아이에 대한 접근 권한이 없습니다.");
		}
	}

}
