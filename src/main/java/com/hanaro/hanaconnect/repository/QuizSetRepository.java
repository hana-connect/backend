package com.hanaro.hanaconnect.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hanaro.hanaconnect.entity.QuizSet;

public interface QuizSetRepository extends JpaRepository<QuizSet, Long> {

	// 오늘 퀴즈 조회
	Optional<QuizSet> findByChildIdAndQuizDate(Long childId, LocalDate quizDate);
}
