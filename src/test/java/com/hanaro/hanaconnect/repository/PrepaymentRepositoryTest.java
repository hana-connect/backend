package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Prepayment;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountCryptoService.class)
@ActiveProfiles("test")
class PrepaymentRepositoryTest {

	@Autowired
	private PrepaymentRepository prepaymentRepository;
	@Autowired
	private AccountCryptoService accountCryptoService;

	@Autowired
	private EntityManager entityManager;

	private static long accountSeq = 30000000000L;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Test
	void findByAccountId_success() {
		Member member = saveMember("김청약", MemberRole.KID);
		Account account = saveAccount(member);

		Prepayment prepayment = Prepayment.builder()
			.totalAmount(new BigDecimal("300000"))
			.installmentCount(3)
			.installmentAmount(new BigDecimal("100000"))
			.startRound(1)
			.endRound(3)
			.account(account)
			.build();

		entityManager.persist(prepayment);
		entityManager.flush();
		entityManager.clear();

		Optional<Prepayment> result = prepaymentRepository.findByAccountId(account.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getAccount().getId()).isEqualTo(account.getId());
		assertThat(result.get().getInstallmentCount()).isEqualTo(3);
	}

	@Test
	void findByAccountId_empty() {
		Member member = saveMember("홍길동", MemberRole.KID);
		Account account = saveAccount(member);

		entityManager.flush();
		entityManager.clear();

		Optional<Prepayment> result = prepaymentRepository.findByAccountId(account.getId());

		assertThat(result).isEmpty();
	}

	private Member saveMember(String name, MemberRole memberRole) {
		Member member = Member.builder()
			.name(name)
			.password(passwordEncoder.encode("123456"))
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount(generateAccount())
			.walletMoney(new BigDecimal("50000"))
			.memberRole(memberRole)
			.role(Role.USER)
			.build();

		entityManager.persist(member);
		return member;
	}

	private Account saveAccount(Member member) {
		String rawAccountNumber = generateAccount();

		Account account = Account.builder()
			.name("청약 계좌")
			.accountNumber(rawAccountNumber)
			.accountNumberHash(accountCryptoService.encrypt(rawAccountNumber))
			.password(passwordEncoder.encode("1111"))
			.accountType(AccountType.SUBSCRIPTION)
			.balance(new BigDecimal("500000"))
			.totalLimit(new BigDecimal("1000000"))
			.member(member)
			.build();

		entityManager.persist(account);
		return account;
	}

	private String generateAccount() {
		return String.valueOf(accountSeq++);
	}
}
