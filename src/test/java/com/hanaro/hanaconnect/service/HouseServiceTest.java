package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.common.util.HouseLevelCalculator;
import com.hanaro.hanaconnect.dto.house.HouseHistoryResponseDTO;
import com.hanaro.hanaconnect.dto.house.HouseStatusResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.House;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.HouseRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

import jakarta.persistence.EntityNotFoundException;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class HouseServiceTest {

	@Autowired
	private HouseService houseService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private HouseRepository houseRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccountCryptoService accountCryptoService;

	@Autowired
	private AccountHashService accountHashService;

	private Member kidWithHouse;
	private Member kidWithoutHouse;
	private Member relatedParent;
	private Member unrelatedParent;

	private Account relatedParentFreeAccount;
	private Account kidWithHouseSubscriptionAccount;

	private static long virtualAccountSeq = 10000000000L;
	private static long accountNumberSeq = 20000000000L;

	@BeforeEach
	void setUp() {
		kidWithoutHouse = createMember(
			"홍길동",
			LocalDate.of(2010, 1, 2),
			MemberRole.KID
		);

		kidWithHouse = createMember(
			"김청약",
			LocalDate.of(2012, 3, 1),
			MemberRole.KID
		);

		unrelatedParent = createMember(
			"김엄마",
			LocalDate.of(1980, 5, 19),
			MemberRole.PARENT
		);

		relatedParent = createMember(
			"청약할머니",
			LocalDate.of(1958, 7, 15),
			MemberRole.PARENT
		);

		relatedParentFreeAccount = createAccount(
			"청약할머니 입출금 통장",
			"1234",
			AccountType.FREE,
			new BigDecimal("10000000"),
			relatedParent,
			null
		);

		kidWithHouseSubscriptionAccount = createAccount(
			"김청약 주택청약",
			"4321",
			AccountType.SUBSCRIPTION,
			new BigDecimal("5600000"),
			kidWithHouse,
			null
		);

		houseRepository.save(
			House.builder()
				.member(kidWithHouse)
				.account(kidWithHouseSubscriptionAccount)
				.level(3)
				.totalCount(28)
				.monthlyPayment(new BigDecimal("200000"))
				.startDate(LocalDate.of(2024, 1, 25))
				.build()
		);

		relationRepository.save(createRelation(kidWithHouse, relatedParent));
		relationRepository.save(createRelation(relatedParent, kidWithHouse));

		createCheongyakTransactions(relatedParentFreeAccount, kidWithHouseSubscriptionAccount);
	}

	private Member createMember(
		String name,
		LocalDate birthday,
		MemberRole memberRole
	) {
		String rawVirtualAccount = generateVirtualAccount();

		return memberRepository.save(
			Member.builder()
				.name(name)
				.password(passwordEncoder.encode("123456"))
				.birthday(birthday)
				.virtualAccount(accountCryptoService.encrypt(rawVirtualAccount))
				.memberRole(memberRole)
				.role(Role.USER)
				.build()
		);
	}

	private Account createAccount(
		String name,
		String rawPassword,
		AccountType accountType,
		BigDecimal balance,
		Member member,
		BigDecimal totalLimit
	) {
		String rawAccountNumber = generateAccountNumber();

		return accountRepository.save(
			Account.builder()
				.name(name)
				.accountNumber(accountCryptoService.encrypt(rawAccountNumber))
				.accountNumberHash(accountHashService.hash(rawAccountNumber))
				.password(passwordEncoder.encode(rawPassword))
				.accountType(accountType)
				.balance(balance)
				.member(member)
				.totalLimit(totalLimit)
				.build()
		);
	}

	private Relation createRelation(Member member, Member connectMember) {
		return Relation.builder()
			.member(member)
			.connectMember(connectMember)
			.connectMemberRole(connectMember.getMemberRole())
			.build();
	}

	private void createCheongyakTransactions(Account senderAccount, Account receiverAccount) {
		LocalDate startDate = LocalDate.of(2024, 1, 12);

		for (int i = 0; i < 28; i++) {
			LocalDate paymentDate = startDate.plusMonths(i);

			Transaction transaction = Transaction.builder()
				.transactionMoney(new BigDecimal("200000"))
				.transactionBalance(new BigDecimal(200000L * (i + 1)))
				.transactionType(TransactionType.SUBSCRIPTION)
				.senderAccount(senderAccount)
				.receiverAccount(receiverAccount)
				.build();

			transaction.setCreatedAtForInit(paymentDate.atTime(12, 0));

			transactionRepository.save(transaction);
		}
	}

	private String generateVirtualAccount() {
		return String.valueOf(virtualAccountSeq++);
	}

	private String generateAccountNumber() {
		return String.valueOf(accountNumberSeq++);
	}

	@Test
	@DisplayName("아이가 본인 청약 리포트를 조회하면 최근 납입자 기준 personalized 메시지를 반환한다")
	void getHouseStatusByKidTest() {
		HouseStatusResponseDTO result = houseService.getHouseStatus(kidWithHouse.getId(), null);

		assertThat(result).isNotNull();
		assertThat(result.getMemberId()).isEqualTo(kidWithHouse.getId());
		assertThat(result.getTotalCount()).isEqualTo(28);
		assertThat(result.getMonthlyPayment()).isEqualByComparingTo("200000");
		assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 25));
		assertThat(result.getLevel())
			.isEqualTo(HouseLevelCalculator.calculateLevel(result.getTotalCount()));
		assertThat(result.getGauge())
			.isEqualTo(HouseLevelCalculator.calculateGauge(result.getTotalCount()));
		assertThat(result.getMessage()).contains("할머니");
	}

	@Test
	@DisplayName("조부모가 연결된 아이의 청약 리포트를 조회하면 기본 메시지를 반환한다")
	void getHouseStatusByParentTest() {
		HouseStatusResponseDTO result = houseService.getHouseStatus(relatedParent.getId(), kidWithHouse.getId());

		assertThat(result).isNotNull();
		assertThat(result.getMemberId()).isEqualTo(kidWithHouse.getId());
		assertThat(result.getTotalCount()).isEqualTo(28);
		assertThat(result.getMessage()).isNotNull();
		assertThat(result.getMessage()).doesNotContain("할머니");
	}

	@Test
	@DisplayName("청약이 없는 아이가 조회하면 level 0 상태를 반환한다")
	void getHouseStatusWithoutHouseTest() {
		HouseStatusResponseDTO result = houseService.getHouseStatus(kidWithoutHouse.getId(), null);

		assertThat(result).isNotNull();
		assertThat(result.getMemberId()).isEqualTo(kidWithoutHouse.getId());
		assertThat(result.getLevel()).isEqualTo(0);
		assertThat(result.getGauge()).isEqualTo(0);
		assertThat(result.getTotalCount()).isNull();
		assertThat(result.getMonthlyPayment()).isNull();
		assertThat(result.getStartDate()).isNull();
		assertThat(result.getMessage()).isNull();
	}

	@Test
	@DisplayName("조부모가 kidId 없이 요청하면 예외가 발생한다")
	void getHouseStatusWithoutKidIdByParentTest() {
		assertThatThrownBy(() -> houseService.getHouseStatus(relatedParent.getId(), null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("kidId는 필수입니다.");
	}

	@Test
	@DisplayName("조부모가 관계 없는 아이를 조회하면 접근 예외가 발생한다")
	void getHouseStatusByUnrelatedParentTest() {
		assertThatThrownBy(() -> houseService.getHouseStatus(unrelatedParent.getId(), kidWithHouse.getId()))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("해당 아이의 정보에 접근할 수 없습니다.");
	}

	@Test
	@DisplayName("존재하지 않는 requesterId로 조회하면 예외가 발생한다")
	void getHouseStatusByInvalidRequesterTest() {
		assertThatThrownBy(() -> houseService.getHouseStatus(999999L, null))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("회원 정보를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("청약 히스토리를 정상적으로 조회한다 (아이 기준)")
	void getHouseHistoryByKidTest() {
		HouseHistoryResponseDTO result = houseService.getHouseHistory(kidWithHouse.getId(), null);

		assertThat(result).isNotNull();
		assertThat(result.getHistories()).isNotEmpty();

		if (result.getHistories().size() > 1) {
			assertThat(result.getHistories().get(0).getTotalCount())
				.isGreaterThan(result.getHistories().get(1).getTotalCount());
		}

		assertThat(result.getHistories())
			.allMatch(h -> h.isFirst() || h.getTotalCount() % 12 == 0);

		result.getHistories().forEach(h ->
			assertThat(h.getLevel())
				.isEqualTo(HouseLevelCalculator.calculateLevel(h.getTotalCount()))
		);
	}

	@Test
	@DisplayName("조부모가 연결된 아이의 히스토리를 조회할 수 있다")
	void getHouseHistoryByParentTest() {
		HouseHistoryResponseDTO result = houseService.getHouseHistory(relatedParent.getId(), kidWithHouse.getId());

		assertThat(result).isNotNull();
		assertThat(result.getHistories()).isNotEmpty();
	}

	@Test
	@DisplayName("조부모가 kidId 없이 히스토리를 조회하면 예외 발생")
	void getHouseHistoryWithoutKidIdTest() {
		assertThatThrownBy(() -> houseService.getHouseHistory(relatedParent.getId(), null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("kidId는 필수입니다.");
	}
}
