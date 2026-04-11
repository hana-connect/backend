package com.hanaro.hanaconnect.entity;

import com.hanaro.hanaconnect.common.entity.BaseEntity;
import com.hanaro.hanaconnect.common.enums.QuizQuestionStatus;

import jakarta.persistence.Column;
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

	// json : mysql이면 가능
	@Column(name = "choices", nullable = false, columnDefinition = "JSON")
	private String choices;

	@Column(name = "correct_index", nullable = false)
	private Integer correctIndex;

	@Column(name = "selected_index")
	private Integer selectedIndex;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private QuizQuestionStatus status;

	@Column(name = "hint", length = 255)
	private String hint;

	// 답 제출 확인
	public void submitAnswer(Integer selectedIndex){
		this.selectedIndex = selectedIndex;

		if(this.correctIndex.equals(selectedIndex)){
			this.status = QuizQuestionStatus.CORRECT;
		} else {
			this.status = QuizQuestionStatus.WRONG;
		}

	}

	// 중간에 이탈 시 -> 오답 처리
	public void markWrong() {
		this.status = QuizQuestionStatus.WRONG;
	}

	// quizSet 연결 메서드
	public void assignQuizSet(QuizSet quizSet) {
		this.quizSet = quizSet;
	}

	// 더미테스트!
	public static QuizQuestion create(
		Integer questionOrder,
		String question,
		String choices,
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
