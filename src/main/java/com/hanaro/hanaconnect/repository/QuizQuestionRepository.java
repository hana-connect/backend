package com.hanaro.hanaconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.QuizQuestion;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

	// 문제 리스트 순서대로 조회
	List<QuizQuestion> findByQuizSetIdOrderByQuestionOrderAsc(Long quizSetId);

	// 특정 문제 조회
	Optional<QuizQuestion> findByQuizSetIdAndQuestionOrder(Long quizSetId, Integer questionOrder);
}
