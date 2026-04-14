package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Transaction;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TransactionRepositoryTest {

	@Autowired
	TransactionRepository transactionRepository;

	@Autowired
	EntityManager entityManager;

	private static long accountSeq = 10000000000L;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Test
	void save_success() {
		Member senderMember = saveMember("부모", MemberRole.PARENT);
		Member receiverMember = saveMember("김꼬마", MemberRole.KID);

		Account senderAccount = saveAccount(senderMember, AccountType.FREE, new BigDecimal("50000"));
		Account receiverAccount = saveAccount(receiverMember, AccountType.SAVINGS, new BigDecimal("10000"));

		Transaction transaction = Transaction.builder()
			.transactionMoney(new BigDecimal("10000"))
			.transactionBalance(new BigDecimal("40000"))
			.transactionType(TransactionType.SAVINGS_TRANSFER)
			.senderAccount(senderAccount)
			.receiverAccount(receiverAccount)
			.build();

		Transaction savedTransaction = transactionRepository.save(transaction);

		assertThat(savedTransaction.getId()).isNotNull();
		assertThat(savedTransaction.getTransactionMoney()).isEqualByComparingTo("10000");
		assertThat(savedTransaction.getTransactionBalance()).isEqualByComparingTo("40000");
		assertThat(savedTransaction.getTransactionType()).isEqualTo(TransactionType.SAVINGS_TRANSFER);
		assertThat(savedTransaction.getSenderAccount().getId()).isEqualTo(senderAccount.getId());
		assertThat(savedTransaction.getReceiverAccount().getId()).isEqualTo(receiverAccount.getId());
	}

	@Test
	void findById_success() {
		Member senderMember = saveMember("부모", MemberRole.PARENT);
		Member receiverMember = saveMember("김꼬마", MemberRole.KID);

		Account senderAccount = saveAccount(senderMember, AccountType.FREE, new BigDecimal("50000"));
		Account receiverAccount = saveAccount(receiverMember, AccountType.SAVINGS, new BigDecimal("10000"));

		Transaction transaction = Transaction.builder()
			.transactionMoney(new BigDecimal("15000"))
			.transactionBalance(new BigDecimal("35000"))
			.transactionType(TransactionType.SAVINGS_TRANSFER)
			.senderAccount(senderAccount)
			.receiverAccount(receiverAccount)
			.build();

		Transaction savedTransaction = transactionRepository.save(transaction);

		entityManager.flush();
		entityManager.clear();

		Transaction foundTransaction = transactionRepository.findById(savedTransaction.getId())
			.orElseThrow();

		assertThat(foundTransaction.getTransactionMoney()).isEqualByComparingTo("15000");
		assertThat(foundTransaction.getTransactionBalance()).isEqualByComparingTo("35000");
		assertThat(foundTransaction.getTransactionType()).isEqualTo(TransactionType.SAVINGS_TRANSFER);
		assertThat(foundTransaction.getSenderAccount().getId()).isEqualTo(senderAccount.getId());
		assertThat(foundTransaction.getReceiverAccount().getId()).isEqualTo(receiverAccount.getId());
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

	private Account saveAccount(Member member, AccountType accountType, BigDecimal balance) {
		Account account = Account.builder()
			.name(accountType == AccountType.FREE ? "자유 입출금" : "아이 적금")
			.accountNumber(generateAccount())
			.password(passwordEncoder.encode("1111"))
			.accountType(accountType)
			.balance(balance)
			.totalLimit(new BigDecimal("100000"))
			.member(member)
			.build();

		entityManager.persist(account);
		return account;
	}

	private String generateAccount() {
		return String.valueOf(accountSeq++);
	}

	@Test
	void sumMonthlyPaymentAmount_success() {
		Member parentMember = saveMember("부모", MemberRole.PARENT);
		Member kidMember = saveMember("김청약", MemberRole.KID);

		Account senderAccount = saveAccount(parentMember, AccountType.FREE, new BigDecimal("1000000"));
		Account subscriptionAccount = saveAccount(kidMember, AccountType.SUBSCRIPTION, new BigDecimal("500000"));

		LocalDateTime thisMonthPayment1 = YearMonth.now().atDay(5).atTime(12, 0);
		LocalDateTime thisMonthPayment2 = YearMonth.now().atDay(10).atTime(12, 0);
		LocalDateTime lastMonthPayment = YearMonth.now().minusMonths(1).atDay(20).atTime(12, 0);

		Transaction transaction1 = Transaction.builder()
			.transactionMoney(new BigDecimal("100000"))
			.transactionBalance(new BigDecimal("600000"))
			.transactionType(TransactionType.DEPOSIT)
			.senderAccount(senderAccount)
			.receiverAccount(subscriptionAccount)
			.build();
		transaction1.setCreatedAtForInit(thisMonthPayment1);

		Transaction transaction2 = Transaction.builder()
			.transactionMoney(new BigDecimal("200000"))
			.transactionBalance(new BigDecimal("800000"))
			.transactionType(TransactionType.DEPOSIT)
			.senderAccount(senderAccount)
			.receiverAccount(subscriptionAccount)
			.build();
		transaction2.setCreatedAtForInit(thisMonthPayment2);

		Transaction transaction3 = Transaction.builder()
			.transactionMoney(new BigDecimal("300000"))
			.transactionBalance(new BigDecimal("1100000"))
			.transactionType(TransactionType.DEPOSIT)
			.senderAccount(senderAccount)
			.receiverAccount(subscriptionAccount)
			.build();
		transaction3.setCreatedAtForInit(lastMonthPayment);

		entityManager.persist(transaction1);
		entityManager.persist(transaction2);
		entityManager.persist(transaction3);

		entityManager.flush();
		entityManager.clear();

		LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
		LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

		BigDecimal result = transactionRepository.sumMonthlyPaymentAmount(
			subscriptionAccount.getId(),
			startOfMonth,
			endOfMonth
		);

		assertThat(result).isEqualByComparingTo("300000");
	}

	@Test
	void sumMonthlyPaymentAmount_returnsZero_whenNoTransactionInThisMonth() {
		Member parentMember = saveMember("부모", MemberRole.PARENT);
		Member kidMember = saveMember("홍길동", MemberRole.KID);

		Account senderAccount = saveAccount(parentMember, AccountType.FREE, new BigDecimal("1000000"));
		Account subscriptionAccount = saveAccount(kidMember, AccountType.SUBSCRIPTION, new BigDecimal("500000"));

		LocalDateTime lastMonthPayment = YearMonth.now().minusMonths(1).atDay(15).atTime(12, 0);

		Transaction transaction = Transaction.builder()
			.transactionMoney(new BigDecimal("150000"))
			.transactionBalance(new BigDecimal("650000"))
			.transactionType(TransactionType.DEPOSIT)
			.senderAccount(senderAccount)
			.receiverAccount(subscriptionAccount)
			.build();
		transaction.setCreatedAtForInit(lastMonthPayment);

		entityManager.persist(transaction);

		entityManager.flush();
		entityManager.clear();

		LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
		LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

		BigDecimal result = transactionRepository.sumMonthlyPaymentAmount(
			subscriptionAccount.getId(),
			startOfMonth,
			endOfMonth
		);

		assertThat(result).isEqualByComparingTo("0");
	}

  
}
