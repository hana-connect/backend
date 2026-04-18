package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
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
			.birthday(LocalDate.of(2000, 1, 1))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build());

		Account account = accountRepository.save(Account.builder()
			.member(member)
			.name("부모 입출금 통장")
			.accountNumber("12345678901")
			.accountNumberHash("hashed-12345678901")
			.password("1234")
			.accountType(AccountType.FREE)
			.balance(BigDecimal.valueOf(100000))
			.build());

		Optional<Account> result = accountRepository.findByMemberIdAndAccountType(
			member.getId(),
			AccountType.FREE
		);

		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(account.getId());
	}

	@Test
	@DisplayName("회원 + 계좌타입으로 계좌 조회 실패 -> empty")
	void findByMemberIdAndAccountType_notFound() {
		Member member = memberRepository.save(Member.builder()
			.name("홍길동")
			.password("1234")
			.virtualAccount("22222222222")
			.birthday(LocalDate.of(2000, 1, 1))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build());

		accountRepository.save(Account.builder()
			.member(member)
			.name("부모 입출금 통장")
			.accountNumber("99999999999")
			.accountNumberHash("hashed-99999999999")
			.password("1234")
			.accountType(AccountType.FREE)
			.balance(BigDecimal.valueOf(100000))
			.build());

		Optional<Account> result = accountRepository.findByMemberIdAndAccountType(
			member.getId(),
			AccountType.DEPOSIT
		);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("회원 ID와 계좌 타입으로 만기된(isEnd=true) 계좌 목록을 ID 오름차순으로 조회 성공")
	void findByMemberIdAndAccountTypeAndIsEndTrueOrderByIdAsc_success() {
		Member member = memberRepository.save(Member.builder()
			.name("홍길동")
			.password("1234")
			.virtualAccount("11122233344")
			.birthday(LocalDate.of(2010, 1, 1))
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build());

		accountRepository.save(Account.builder()
			.member(member)
			.name("만기적금1")
			.accountNumber("11110111110")
			.accountNumberHash("hashed-11110111110")
			.password("1234")
			.accountType(AccountType.SAVINGS)
			.balance(BigDecimal.valueOf(1000))
			.isEnd(true)
			.build());

		accountRepository.save(Account.builder()
			.member(member)
			.name("만기적금2")
			.accountNumber("22200220002")
			.accountNumberHash("hashed-22200220002")
			.password("1234")
			.accountType(AccountType.SAVINGS)
			.balance(BigDecimal.valueOf(2000))
			.isEnd(true)
			.build());

		accountRepository.save(Account.builder()
			.member(member)
			.name("진행중적금")
			.accountNumber("33300333002")
			.accountNumberHash("hashed-33300333002")
			.password("1234")
			.accountType(AccountType.SAVINGS)
			.balance(BigDecimal.valueOf(3000))
			.isEnd(false)
			.build());

		accountRepository.save(Account.builder()
			.member(member)
			.name("입출금통장")
			.accountNumber("44404440444")
			.accountNumberHash("hashed-44404440444")
			.password("1234")
			.accountType(AccountType.FREE)
			.balance(BigDecimal.valueOf(4000))
			.isEnd(true)
			.build());

		List<Account> result = accountRepository.findByMemberIdAndAccountTypeAndIsEndTrueOrderByIdAsc(
			member.getId(),
			AccountType.SAVINGS
		);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("만기적금1");
		assertThat(result.get(1).getName()).isEqualTo("만기적금2");
		assertThat(result).extracting("isEnd").containsOnly(true);
	}
}
