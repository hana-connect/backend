package com.hanaro.hanaconnect;

import org.springframework.boot.ApplicationRunner;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.security.AccountCryptoService;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InitLoader implements ApplicationRunner {

	private final MemberRepository memberRepository;
	private final AccountCryptoService accountCryptoService;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(@Nullable ApplicationArguments args) {

		// 이미 회원이 있으면 추가하지 않음
		if (memberRepository.count() > 0) {
			return;
		}

		String rawPassword = "123456";
		String rawAccount = "11122223333";
		String encryptedAccount = accountCryptoService.encrypt(rawAccount);

		String encodedPassword = passwordEncoder.encode(rawPassword);

		Member member = Member.builder()
			.name("홍길동")
			.password(encodedPassword)
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount(encryptedAccount)
			.walletMoney(BigDecimal.ZERO)
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build();

		memberRepository.save(member);
	}
}
