package com.hanaro.hanaconnect.entity;

import java.util.List;

import com.hanaro.hanaconnect.common.converter.StringListConverter;
import com.hanaro.hanaconnect.common.entity.BaseEntity;
import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "quiz_question",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_quiz_question_quiz_set_id_question_order",
			columnNames = {"quiz_set_id", "question_order"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuizQuestion extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "quiz_set_id", nullable = false)
	private QuizSet quizSet;

	@Column(name = "question_order", nullable = false)
	private Integer questionOrder;

	@Column(name = "question", nullable = false, columnDefinition = "TEXT")
	private String question;

	// json : mysql이면 가능 / convert :
	@Convert(converter = StringListConverter.class)
	@Column(name = "choices", nullable = false, columnDefinition = "JSON")
	private List<String> choices;

	@Column(name = "correct_index", nullable = false)
	private Integer correctIndex;

	@Column(name = "selected_index")
	private Integer selectedIndex;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private QuizQuestionStatus status;

	@Column(name = "hint", length = 255)
	private String hint;

	// 답 제출
	public void submitAnswer(Integer selectedIndex) {
		if (this.status != QuizQuestionStatus.READY) {
			throw new IllegalStateException("이미 답을 제출한 문제입니다.");
		}

		this.selectedIndex = selectedIndex;

		if (this.correctIndex.equals(selectedIndex)) {
			this.status = QuizQuestionStatus.CORRECT;
		} else {
			this.status = QuizQuestionStatus.WRONG;
		}
	}

	// 중간 이탈 시 오답 처리
	public void markWrong() {
		if (this.status != QuizQuestionStatus.READY) {
			throw new IllegalStateException("이미 처리된 문제입니다.");
		}

		this.status = QuizQuestionStatus.WRONG;
	}

	// 정답 텍스트 반환
	public String getCorrectAnswer() {
		return this.choices.get(this.correctIndex);
	}

	// quizSet 연결
	public void assignQuizSet(QuizSet quizSet) {
		this.quizSet = quizSet;
	}

	// 생성 메서드
	public static QuizQuestion create(
		Integer questionOrder,
		String question,
		List<String> choices,
		Integer correctIndex,
		String hint
	) {
		QuizQuestion quizQuestion = new QuizQuestion();
		quizQuestion.questionOrder = questionOrder;
		quizQuestion.question = question;
		quizQuestion.choices = choices;
		quizQuestion.correctIndex = correctIndex;
		quizQuestion.selectedIndex = null;
		quizQuestion.status = QuizQuestionStatus.READY;
		quizQuestion.hint = hint;
		return quizQuestion;
	}
}
