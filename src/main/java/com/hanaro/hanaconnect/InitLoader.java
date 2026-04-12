package com.hanaro.hanaconnect;

import org.springframework.boot.ApplicationRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.security.AccountCryptoService;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Mission;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InitLoader implements ApplicationRunner {

	private final MemberRepository memberRepository;
	private final AccountCryptoService accountCryptoService;
	private final PasswordEncoder passwordEncoder;
	private final RelationRepository relationRepository;
	private final MissionRepository missionRepository;

	@Override
	public void run(@Nullable ApplicationArguments args) {

		//이미 회원이 있으면 추가하지 않음
		if (memberRepository.count() > 0) {
			return;
		}

		String rawPassword = "123456";
		String rawAccount = "11122223333";
		String encryptedAccount = accountCryptoService.encrypt(rawAccount);
		String parentAccount = accountCryptoService.encrypt("99988887777");

		String encodedPassword = passwordEncoder.encode(rawPassword);

		// 부모 생성
		Member parent = Member.builder()
			.name("부모")
			.password(encodedPassword)
			.birthday(LocalDate.of(1980, 1, 1))
			.virtualAccount(parentAccount)
			.walletMoney(BigDecimal.ZERO)
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build();

		memberRepository.save(parent);

		Member kid = Member.builder()
			.name("아이")
			.password(encodedPassword)
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount(encryptedAccount)
			.walletMoney(BigDecimal.ZERO)
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build();

		memberRepository.save(kid);

		// 부모-아이 관계 생성
		Relation relation = Relation.builder()
			.parent(parent)
			.kid(kid)
			.build();

		relationRepository.save(relation);

		missionRepository.saveAll(List.of(
			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("부모님께 인사하기")
				.isCompleted(true)
				.build(),

			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("심부름 다녀오기")
				.isCompleted(true)
				.build(),

			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("용돈 기록 작성하기")
				.isCompleted(true)
				.build(),

			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("방 정리하기")
				.isCompleted(true)
				.build(),

			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("식사 후 설거지 돕기")
				.isCompleted(true)
				.build(),

			Mission.builder()
				.kid(kid)
				.parent(parent)
				.name("오늘 소비 내역 확인하기")
				.isCompleted(true)
				.build()
		));
	}
}
