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
	 * - 지금은 뼈대만 작성
	 * - 이후 AI 생성 브랜치에서 문제 3개 생성 연결
	 */
	@Transactional
	public QuizSet createTodayQuiz(Long childId, LocalDate quizDate) {
		QuizSet quizSet = QuizSet.create(childId, quizDate, 3);

		QuizQuestion q1 = QuizQuestion.create(
			1,
			"다음 중 아이가 최근 가장 좋아한 활동은 무엇일까요?",
			"[\"그림 그리기\", \"블록 놀이\", \"영상 통화\", \"산책\"]",
			2,
			"가족과 함께한 활동이에요."
		);

		QuizQuestion q2 = QuizQuestion.create(
			2,
			"아이와 최근 함께한 일로 알맞은 것은 무엇일까요?",
			"[\"책 읽기\", \"사진 찍기\", \"외식하기\", \"퍼즐 맞추기\"]",
			1,
			"기념으로 남길 수 있는 활동이에요."
		);

		QuizQuestion q3 = QuizQuestion.create(
			3,
			"다음 중 아이가 좋아하는 간식은 무엇일까요?",
			"[\"바나나\", \"딸기\", \"쿠키\", \"우유\"]",
			0,
			"과일이에요."
		);

		quizSet.addQuestion(q1);
		quizSet.addQuestion(q2);
		quizSet.addQuestion(q3);

		return quizSetRepository.save(quizSet);
	}

	// 현재 풀어야 할 문제 조회 -> READY 상태 문제 중 questionOrder가 가장 작은 문제 반환
	public QuizQuestion getCurrentQuestion(Long quizSetId) {
		List<QuizQuestion> questions = quizQuestionRepository.findByQuizSetIdOrderByQuestionOrderAsc(quizSetId);

		return questions.stream()
			.filter(question -> question.getStatus() == QuizQuestionStatus.READY)
			.findFirst()
			.orElse(null);
	}


	// 답 제출 -> question 상태 변경, quizSet solvedCount 증가
	@Transactional
	public void submitAnswer(Long quizSetId, Integer questionOrder, Integer selectedIndex) {
		QuizSet quizSet = quizSetRepository.findById(quizSetId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀴즈 세트입니다."));

		QuizQuestion question = quizQuestionRepository.findByQuizSetIdAndQuestionOrder(quizSetId, questionOrder)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다."));

		question.submitAnswer(selectedIndex);
		quizSet.increaseSolvedCount();
	}

	// 문제 이탈 처리
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

	// 완료 여부 확인
	public boolean isCompleted(Long quizSetId) {
		QuizSet quizSet = quizSetRepository.findById(quizSetId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀴즈 세트입니다."));

		return quizSet.getStatus() == QuizSetStatus.COMPLETED;
	}

}
