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
@Table(name = "mission")
public class Mission extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "is_completed", nullable = false)
	@Builder.Default
	private Boolean isCompleted = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id", nullable = false)
	private Member parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kid_id", nullable = false)
	private Member kid;
}
