package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	@DisplayName("회원 + 계좌타입으로 계좌 조회 성공")
	void findByMemberIdAndAccountType_success() {
		Member member = memberRepository.save(Member.builder()
			.name("홍길동")
			.password("1234")
			.virtualAccount("11111111111")
			.walletMoney(BigDecimal.ZERO)
			.birthday(LocalDate.of(2000, 1, 1))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build());

		Account account = accountRepository.save(Account.builder()
			.member(member)
			.name("부모 입출금 통장")
			.accountNumber("12345678901")
			.password("1234")
			.accountType(AccountType.FREE)
			.balance(BigDecimal.valueOf(100000))
			.build());

		Optional<Account> result =
			accountRepository.findByMemberIdAndAccountType(
				member.getId(),
				AccountType.FREE
			);

		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(account.getId());
	}

	@Test
	@DisplayName("회원 + 계좌타입으로 계좌 조회 실패 → empty")
	void findByMemberIdAndAccountType_notFound() {
		// given
		Member member = memberRepository.save(Member.builder()
			.name("홍길동")
			.password("1234")
			.virtualAccount("22222222222")
			.walletMoney(BigDecimal.ZERO)
			.birthday(LocalDate.of(2000, 1, 1))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build());

		// when
		Optional<Account> result =
			accountRepository.findByMemberIdAndAccountType(
				member.getId(),
				AccountType.DEPOSIT
			);

		// then
		assertThat(result).isEmpty();
	}
}
