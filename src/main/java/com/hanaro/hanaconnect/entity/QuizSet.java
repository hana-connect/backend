package com.hanaro.hanaconnect.entity;

import java.time.LocalDate;
import java.util.ArrayList;

import com.hanaro.hanaconnect.common.entity.BaseEntity;
import com.hanaro.hanaconnect.common.enums.QuizSetStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Entity
@Table(
	name = "quiz_set",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_quiz_set_child_id_quiz_date",
			columnNames = {"child_id", "quiz_date"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizSet extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "child_id", nullable = false)
	private Long childId;

	@Column(name = "quiz_date", nullable = false)
	private LocalDate quizDate;

	// QuizSet 안에 QuizQusetion list
	@OneToMany(mappedBy = "quizSet", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<QuizQuestion> questions = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private QuizSetStatus status;

	@Column(name = "total_count", nullable = false)
	private Integer totalCount;

	@Column(name = "solved_count", nullable = false)
	private Integer solvedCount;

	// 문제 추가
	public void addQuestion(QuizQuestion question){
		this.questions.add(question);
		question.assignQuizSet(this);
	}

	// 푼 문제수 증가
	public void increaseSolvedCount(){
		this.solvedCount += 1;
		updateStatus();
	}

	// 상태 업데이트
	public void updateStatus() {
		if (this.solvedCount == 0) {
			this.status = QuizSetStatus.NOT_STARTED;
		} else if (this.solvedCount < this.totalCount){
			this.status = QuizSetStatus.IN_PROGRESS;
		} else {
			this.status = QuizSetStatus.COMPLETED;
		}
	}

	// 생성 메서드
	public static QuizSet create(Long childId, LocalDate quizDate, Integer totalCount) {
		QuizSet quizSet = new QuizSet();
		quizSet.childId = childId;
		quizSet.quizDate = quizDate;
		quizSet.status = QuizSetStatus.NOT_STARTED;
		quizSet.totalCount = totalCount;
		quizSet.solvedCount = 0;
		return quizSet;
	}
}
