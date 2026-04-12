package com.hanaro.hanaconnect.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;
import com.hanaro.hanaconnect.entity.QuizQuestion;
import com.hanaro.hanaconnect.entity.QuizSet;
import com.hanaro.hanaconnect.repository.QuizQuestionRepository;
import com.hanaro.hanaconnect.repository.QuizSetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

	private final QuizSetRepository quizSetRepository;
	private final QuizQuestionRepository quizQuestionRepository;
	private final QuizGenerationService quizGenerationService;

	/*
	 * 오늘 퀴즈 조회
	 * - 있으면 반환
	 * - 없으면 생성 (추후 AI generation 로직 연결)
	 */
	@Transactional
	public QuizSet getOrCreateTodayQuiz(Long childId) {
		LocalDate today = LocalDate.now();

		return quizSetRepository.findByChildIdAndQuizDate(childId, today)
			.orElseGet(() -> createTodayQuiz(childId, today));
	}

	/*
	 * 오늘 퀴즈 생성
	 * - 현재는 더미 데이터로 3문제 생성
	 * - 이후 AI 생성 브랜치에서 문제 생성 로직으로 교체 예정
	 */
	@Transactional
	public QuizSet createTodayQuiz(Long childId, LocalDate quizDate) {
		return quizGenerationService.generateTodayQuiz(childId, quizDate);
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
