package com.hanaro.hanaconnect.entity;

import com.hanaro.hanaconnect.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "phone_name",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"who_id", "whom_id"})
	}
)
public class PhoneName extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(name = "whom_name", nullable = false, length = 50)
	private String whomName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "who_id", nullable = false)
	private Member who;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "whom_id", nullable = false)
	private Member whom;
}
