package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.account.TerminatedAccountResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AccountServiceTest {

	@Autowired
	private AccountService accountService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccountCryptoService accountCryptoService;

	@Autowired
	private AccountHashService accountHashService;

	@Test
	@DisplayName("나의 만기된 적금 계좌 목록 조회 성공")
	void getTerminatedSavingsSuccessTest() {
		// Given
		Member member = memberRepository.save(Member.builder()
			.name("홍길동")
			.password(passwordEncoder.encode("260420"))
			.virtualAccount("33338888777")
			.memberRole(MemberRole.KID)
			.birthday(LocalDate.of(2000, 1, 1))
			.role(Role.USER)
			.build());

		// 만기된 적금 계좌 저장
		Account savedAccount = accountRepository.save(Account.builder()
			.member(member)
			.name("369 행복 적금")
			.accountNumber(accountCryptoService.encrypt("99988877700"))
			.accountNumberHash(accountHashService.hash("99988877700"))
			.password(passwordEncoder.encode("1234"))
			.accountType(AccountType.SAVINGS)
			.balance(BigDecimal.valueOf(50000))
			.isEnd(true)
			.build());

		// When
		List<TerminatedAccountResponseDTO> result = accountService.getTerminatedSavings(member.getId());

		// Then
		assertThat(result).isNotEmpty();
		assertThat(result).hasSize(1);

		TerminatedAccountResponseDTO response = result.get(0);

		assertThat(response.getName()).isEqualTo("369 행복 적금");
		assertThat(response.getAccountNumber()).isEqualTo("999-8887-7700");
		assertThat(response.getAccountId()).isNotNull();
		assertThat(response.getAccountId()).isEqualTo(savedAccount.getId());
	}
}
