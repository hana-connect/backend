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

import com.hanaro.hanaconnect.common.config.TestSecurityConfig;
import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.enums.Status;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Prepayment;
import com.hanaro.hanaconnect.entity.PrepaymentDetail;
import com.hanaro.hanaconnect.service.AccountHashService;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AccountCryptoService.class, AccountHashService.class})
@ActiveProfiles("test")
class PrepaymentDetailRepositoryTest {

	@Autowired
	private PrepaymentDetailRepository prepaymentDetailRepository;

	@Autowired
	private AccountCryptoService accountCryptoService;

	@Autowired
	private AccountHashService accountHashService;

	@Autowired
	private EntityManager entityManager;

	private static long accountSeq = 20000000000L;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Test
	void findMaxRoundNoByAccountId_success() {
		Member member = saveMember("김청약", MemberRole.KID);
		Account subscriptionAccount = saveAccount(member, AccountType.SUBSCRIPTION, new BigDecimal("500000"));
		Prepayment prepayment = savePrepayment(subscriptionAccount);

		PrepaymentDetail detail1 = PrepaymentDetail.builder()
			.roundNo(1)
			.dueMonth(LocalDate.of(2026, 4, 1))
			.amount(new BigDecimal("100000"))
			.status(Status.COMPLETED)
			.prepayment(prepayment)
			.account(subscriptionAccount)
			.build();

		PrepaymentDetail detail2 = PrepaymentDetail.builder()
			.roundNo(3)
			.dueMonth(LocalDate.of(2026, 6, 1))
			.amount(new BigDecimal("100000"))
			.status(Status.SCHEDULED)
			.prepayment(prepayment)
			.account(subscriptionAccount)
			.build();

		PrepaymentDetail detail3 = PrepaymentDetail.builder()
			.roundNo(2)
			.dueMonth(LocalDate.of(2026, 5, 1))
			.amount(new BigDecimal("100000"))
			.status(Status.SCHEDULED)
			.prepayment(prepayment)
			.account(subscriptionAccount)
			.build();

		entityManager.persist(detail1);
		entityManager.persist(detail2);
		entityManager.persist(detail3);

		entityManager.flush();
		entityManager.clear();

		Integer result = prepaymentDetailRepository.findMaxRoundNoByAccountId(subscriptionAccount.getId())
			.orElseThrow();

		assertThat(result).isEqualTo(3);
	}

	@Test
	void findMaxRoundNoByAccountId_empty() {
		Member member = saveMember("홍길동", MemberRole.KID);
		Account subscriptionAccount = saveAccount(member, AccountType.SUBSCRIPTION, new BigDecimal("300000"));
		savePrepayment(subscriptionAccount);

		entityManager.flush();
		entityManager.clear();

		Optional<Integer> result = prepaymentDetailRepository.findMaxRoundNoByAccountId(subscriptionAccount.getId());

		assertThat(result).isEmpty();
	}

	private Member saveMember(String name, MemberRole memberRole) {
		Member member = Member.builder()
			.name(name)
			.password(passwordEncoder.encode("260420"))
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount(generateAccount())
			.memberRole(memberRole)
			.role(Role.USER)
			.build();

		entityManager.persist(member);
		return member;
	}

	private Account saveAccount(Member member, AccountType accountType, BigDecimal balance) {
		String rawAccountNumber = generateAccount();

		Account account = Account.builder()
			.name("청약 계좌")
			.accountNumber(rawAccountNumber)
			.accountNumberHash(accountHashService.hash(rawAccountNumber))
			.password(passwordEncoder.encode("1111"))
			.accountType(accountType)
			.balance(balance)
			.totalLimit(new BigDecimal("1000000"))
			.member(member)
			.build();

		entityManager.persist(account);
		return account;
	}

	private Prepayment savePrepayment(Account account) {
		Prepayment prepayment = Prepayment.builder()
			.totalAmount(new BigDecimal("300000"))
			.installmentCount(3)
			.installmentAmount(new BigDecimal("100000"))
			.startRound(1)
			.endRound(3)
			.account(account)
			.build();

		entityManager.persist(prepayment);
		return prepayment;
	}

	private String generateAccount() {
		return String.valueOf(accountSeq++);
	}
}
