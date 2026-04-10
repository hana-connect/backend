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
@Table(name = "letter")
public class Letter extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(columnDefinition = "TEXT")
	private String content;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaction_id", nullable = false, unique = true)
	private Transaction transaction;
}
