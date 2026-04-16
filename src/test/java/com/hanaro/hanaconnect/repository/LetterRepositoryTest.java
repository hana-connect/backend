package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.dto.RelayHistoryDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Letter;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Transaction;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LetterRepositoryTest {

	@Autowired
	private LetterRepository letterRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Test
	@DisplayName("내가 특정 계좌로 보낸 편지 내역만 정확히 조회되는지 확인 (페이징 적용)")
	void findMyRelayHistorySuccess() {
		// Given
		// 부모 생성
		Member parent = Member.builder()
			.name("엄마")
			.password("1234")
			.birthday(LocalDate.of(1980, 5, 19))
			.virtualAccount("PARENT_V_ACC")
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.walletMoney(BigDecimal.ZERO)
			.build();
		parent = memberRepository.saveAndFlush(parent);

		// 아이 생성
		Member kid = Member.builder()
			.name("아이")
			.password("1234")
			.birthday(LocalDate.of(2015, 1, 1))
			.virtualAccount("KID_V_ACC")
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.walletMoney(BigDecimal.ZERO)
			.build();
		kid = memberRepository.saveAndFlush(kid);

		// Account 생성
		Account momAccount = Account.builder()
			.name("엄마계좌")
			.accountNumber("111222")
			.password("1234")
			.accountType(AccountType.FREE)
			.balance(new BigDecimal("100000"))
			.member(parent)
			.build();
		momAccount = accountRepository.saveAndFlush(momAccount);

		Account kidSavings = Account.builder()
			.name("아이적금")
			.accountNumber("333444")
			.password("1234")
			.accountType(AccountType.SAVINGS)
			.balance(new BigDecimal("50000"))
			.member(kid)
			.build();
		kidSavings = accountRepository.saveAndFlush(kidSavings);

		// Transaction 저장
		Transaction tx = Transaction.builder()
			.senderAccount(momAccount)
			.receiverAccount(kidSavings)
			.transactionMoney(new BigDecimal("10000"))
			.transactionBalance(new BigDecimal("60000"))
			.transactionType(TransactionType.SAVINGS_DEPOSIT)
			.build();
		tx = transactionRepository.saveAndFlush(tx);

		// Letter 저장
		Letter letter = Letter.builder()
			.content("첫 번째 응원 메시지")
			.transaction(tx)
			.build();
		letterRepository.saveAndFlush(letter);

		Pageable pageable = PageRequest.of(0, 12);

		Page<RelayHistoryDTO> result = letterRepository.findMyRelayHistory(
			parent.getId(),
			kidSavings.getId(),
			pageable
		);

		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getMessage()).isEqualTo("첫 번째 응원 메시지");
	}
}
