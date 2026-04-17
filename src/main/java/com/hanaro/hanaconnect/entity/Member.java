package com.hanaro.hanaconnect.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.hanaro.hanaconnect.common.entity.BaseEntity;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;

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
@Table(name = "users")
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(nullable = false, length = 31)
	private String name;

	// 6자리 간편비밀번호를 인코딩한 값 저장
	@Column(nullable = false, length = 255)
	private String password;

	@Column(nullable = false)
	private LocalDate birthday;

	@Column(name = "virtual_account", nullable = false, unique = true, length = 100)
	private String virtualAccount;

	@Column(name = "wallet_money", nullable = false)
	@Builder.Default
	private BigDecimal walletMoney = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(name = "member_role", nullable = false)
	private MemberRole memberRole;

	// 관리자 여부
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	// kid 회원 기준 house 1개
	@OneToOne(mappedBy = "member", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
	private House house;

	// 내가 연결한 계좌들
	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<LinkedAccount> linkedAccounts = new ArrayList<>();

	@OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<Mission> parentMissions = new ArrayList<>();

	@OneToMany(mappedBy = "kid", cascade = CascadeType.REMOVE, orphanRemoval = true)
	@Builder.Default
	private List<Mission> kidMissions = new ArrayList<>();

}
