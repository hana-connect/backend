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

}
