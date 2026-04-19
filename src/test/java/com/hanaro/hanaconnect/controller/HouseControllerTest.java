package com.hanaro.hanaconnect.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.common.security.JwtTokenProvider;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.House;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.HouseRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;
import com.hanaro.hanaconnect.service.AccountHashService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HouseControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private HouseRepository houseRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccountCryptoService accountCryptoService;

	@Autowired
	private AccountHashService accountHashService;

	@Autowired
	private LinkedAccountRepository linkedAccountRepository;

	private static long accountSeq = 77000000000L;

	private Member kidWithHouse;
	private Member kidWithoutHouse;
	private Member relatedParent;
	private Member unrelatedParent;

	private Account kidWithHouseSubscriptionAccount;
	private Account relatedParentWalletAccount;

	private String kidAccessToken;
	private String relatedParentAccessToken;
	private String unrelatedParentAccessToken;

	@BeforeEach
	void setUp() {
		kidWithHouse = createMember(
			"김청약",
			LocalDate.of(2012, 3, 1),
			MemberRole.KID
		);

		kidWithoutHouse = createMember(
			"홍길동",
			LocalDate.of(2010, 1, 2),
			MemberRole.KID
		);

		relatedParent = createMember(
			"청약할머니",
			LocalDate.of(1958, 7, 15),
			MemberRole.PARENT
		);

		unrelatedParent = createMember(
			"김엄마",
			LocalDate.of(1980, 5, 19),
			MemberRole.PARENT
		);

		saveRelation(kidWithHouse, relatedParent);
		saveRelation(relatedParent, kidWithHouse);

		kidWithHouseSubscriptionAccount = createAccount(
			"김청약 주택청약",
			"4321",
			AccountType.SUBSCRIPTION,
			new BigDecimal("5600000"),
			kidWithHouse,
			null
		);

		relatedParentWalletAccount = createAccount(
			"청약할머니 지갑",
			"123456",
			AccountType.WALLET,
			new BigDecimal("990000"),
			relatedParent,
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

		linkedAccountRepository.save(
			LinkedAccount.builder()
				.member(relatedParent)
				.account(kidWithHouseSubscriptionAccount)
				.build()
		);

		createCheongyakTransactions(relatedParentWalletAccount, kidWithHouseSubscriptionAccount);

		kidAccessToken = createAccessToken(kidWithHouse);
		relatedParentAccessToken = createAccessToken(relatedParent);
		unrelatedParentAccessToken = createAccessToken(unrelatedParent);
	}

	@Test
	@DisplayName("아이가 본인 청약 리포트를 조회할 수 있다")
	void getHouseStatusByKidTest() throws Exception {
		mvc.perform(get("/api/house/status")
				.header("Authorization", "Bearer " + kidAccessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 상태 조회 성공"))
			.andExpect(jsonPath("$.data.memberId").value(kidWithHouse.getId()))
			.andExpect(jsonPath("$.data.totalCount").value(28))
			.andExpect(jsonPath("$.data.monthlyPayment").value(200000))
			.andExpect(jsonPath("$.data.message").value(containsString("할머니")))
			.andDo(print());
	}

	@Test
	@DisplayName("조부모가 연결된 아이의 청약 리포트를 조회할 수 있다")
	void getHouseStatusByParentTest() throws Exception {
		mvc.perform(get("/api/house/status")
				.header("Authorization", "Bearer " + relatedParentAccessToken)
				.param("kidId", String.valueOf(kidWithHouse.getId())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 상태 조회 성공"))
			.andExpect(jsonPath("$.data.memberId").value(kidWithHouse.getId()))
			.andExpect(jsonPath("$.data.totalCount").value(28))
			.andExpect(jsonPath("$.data.message").value(not(containsString("할머니"))))
			.andDo(print());
	}

	@Test
	@DisplayName("청약이 없는 아이가 조회하면 level 0 상태를 반환한다")
	void getHouseStatusWithoutHouseTest() throws Exception {
		mvc.perform(get("/api/house/status")
				.header("Authorization", "Bearer " + createAccessToken(kidWithoutHouse)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 상태 조회 성공"))
			.andExpect(jsonPath("$.data.memberId").value(kidWithoutHouse.getId()))
			.andExpect(jsonPath("$.data.level").value(0))
			.andExpect(jsonPath("$.data.gauge").value(0))
			.andExpect(jsonPath("$.data.totalCount").doesNotExist())
			.andDo(print());
	}

	@Test
	@DisplayName("조부모가 kidId 없이 요청하면 400을 반환한다")
	void getHouseStatusWithoutKidIdByParentTest() throws Exception {
		mvc.perform(get("/api/house/status")
				.header("Authorization", "Bearer " + relatedParentAccessToken))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}

	@Test
	@DisplayName("조부모가 관계 없는 아이를 조회하면 403을 반환한다")
	void getHouseStatusByUnrelatedParentTest() throws Exception {
		mvc.perform(get("/api/house/status")
				.header("Authorization", "Bearer " + unrelatedParentAccessToken)
				.param("kidId", String.valueOf(kidWithHouse.getId())))
			.andExpect(status().isForbidden())
			.andDo(print());
	}

	@Test
	@DisplayName("아이가 청약 히스토리를 조회할 수 있다")
	void getHouseHistoryByKidTest() throws Exception {
		mvc.perform(get("/api/house/history")
				.header("Authorization", "Bearer " + kidAccessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 히스토리 조회 성공"))
			.andExpect(jsonPath("$.data.histories").isArray())
			.andExpect(jsonPath("$.data.histories[0].totalCount").exists())
			.andDo(print());
	}

	@Test
	@DisplayName("조부모가 아이의 히스토리를 조회할 수 있다")
	void getHouseHistoryByParentTest() throws Exception {
		mvc.perform(get("/api/house/history")
				.header("Authorization", "Bearer " + relatedParentAccessToken)
				.param("kidId", String.valueOf(kidWithHouse.getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.histories").isArray())
			.andDo(print());
	}

	@Test
	@DisplayName("조부모가 kidId 없이 히스토리를 조회하면 400 반환")
	void getHouseHistoryWithoutKidIdTest() throws Exception {
		mvc.perform(get("/api/house/history")
				.header("Authorization", "Bearer " + relatedParentAccessToken))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("조부모가 관계 없는 아이 히스토리 조회 시 403")
	void getHouseHistoryByUnrelatedParentTest() throws Exception {
		mvc.perform(get("/api/house/history")
				.header("Authorization", "Bearer " + unrelatedParentAccessToken)
				.param("kidId", String.valueOf(kidWithHouse.getId())))
			.andExpect(status().isForbidden());
	}

	private Member createMember(
		String name,
		LocalDate birthday,
		MemberRole memberRole
	) {
		String rawVirtualAccount = generateAccountNumber();

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
				.isReward(false)
				.build()
		);
	}

	private void saveRelation(Member member, Member connectMember) {
		relationRepository.save(
			Relation.builder()
				.member(member)
				.connectMember(connectMember)
				.connectMemberRole(connectMember.getMemberRole())
				.build()
		);
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

	private String createAccessToken(Member member) {
		TokenMemberPrincipal principal = new TokenMemberPrincipal(
			member.getId(),
			member.getName(),
			member.getVirtualAccount(),
			member.getMemberRole(),
			member.getRole()
		);
		return jwtTokenProvider.createAccessToken(principal);
	}

	private String generateAccountNumber() {
		return String.valueOf(accountSeq++);
	}
}
