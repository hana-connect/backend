package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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


  
}
