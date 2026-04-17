package com.hanaro.hanaconnect.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.common.security.JwtTokenProvider;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.entity.Relation;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.MissionRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.service.AccountHashService;

import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	RelationRepository relationRepository;

	@Autowired
	PhoneNameRepository phoneNameRepository;

	@Autowired
	MissionRepository missionRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	AccountCryptoService accountCryptoService;

	@Autowired
	AccountHashService accountHashService;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	private static long accountSeq = 10000000000L;

	private Long kidId;
	private Long parent1Id;
	private Long parent2Id;
	private Long kidFriendId;

	private String kidAccessToken;
	private String parentAccessToken;
	private String kidFriendAccessToken;

	@BeforeEach
	void setUp() {
		String encodedPassword = passwordEncoder.encode("123456");

		Member me = Member.builder()
			.name("김꼬마")
			.password(encodedPassword)
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount(accountCryptoService.encrypt(generateAccount()))
			.walletMoney(new BigDecimal("50000"))
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build();

		Member parent1 = Member.builder()
			.name("김엄마")
			.password(encodedPassword)
			.birthday(LocalDate.of(1980, 5, 19))
			.virtualAccount(accountCryptoService.encrypt(generateAccount()))
			.walletMoney(new BigDecimal("100000"))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build();

		Member parent2 = Member.builder()
			.name("이할머니")
			.password(encodedPassword)
			.birthday(LocalDate.of(1952, 8, 31))
			.virtualAccount(accountCryptoService.encrypt(generateAccount()))
			.walletMoney(new BigDecimal("90000"))
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build();

		Member kidFriend = Member.builder()
			.name("박친구")
			.password(encodedPassword)
			.birthday(LocalDate.of(2011, 3, 10))
			.virtualAccount(accountCryptoService.encrypt(generateAccount()))
			.walletMoney(new BigDecimal("30000"))
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build();

		me = memberRepository.save(me);
		parent1 = memberRepository.save(parent1);
		parent2 = memberRepository.save(parent2);
		kidFriend = memberRepository.save(kidFriend);

		kidId = me.getId();
		parent1Id = parent1.getId();
		parent2Id = parent2.getId();
		kidFriendId = kidFriend.getId();

		// 아이 -> 부모
		relationRepository.save(Relation.builder()
			.member(me)
			.connectMember(parent1)
			.connectMemberRole(parent1.getMemberRole())
			.build());

		relationRepository.save(Relation.builder()
			.member(me)
			.connectMember(parent2)
			.connectMemberRole(parent2.getMemberRole())
			.build());

		// 부모 -> 아이
		relationRepository.save(Relation.builder()
			.member(parent1)
			.connectMember(me)
			.connectMemberRole(me.getMemberRole())
			.build());

		relationRepository.save(Relation.builder()
			.member(parent2)
			.connectMember(me)
			.connectMemberRole(me.getMemberRole())
			.build());

		// 아이 친구 연결
		relationRepository.save(Relation.builder()
			.member(me)
			.connectMember(kidFriend)
			.connectMemberRole(kidFriend.getMemberRole())
			.build());

		phoneNameRepository.save(PhoneName.builder()
			.who(me)
			.whom(parent1)
			.whomName("우리 엄마")
			.build());

		phoneNameRepository.save(PhoneName.builder()
			.who(me)
			.whom(parent2)
			.whomName("할머니")
			.build());

		phoneNameRepository.save(PhoneName.builder()
			.who(me)
			.whom(kidFriend)
			.whomName("친구")
			.build());

		phoneNameRepository.save(PhoneName.builder()
			.who(parent1)
			.whom(parent2)
			.whomName("시어머니")
			.build());

		TokenMemberPrincipal kidPrincipal = new TokenMemberPrincipal(
			me.getId(),
			me.getName(),
			me.getVirtualAccount(),
			me.getMemberRole(),
			me.getRole()
		);

		TokenMemberPrincipal parentPrincipal = new TokenMemberPrincipal(
			parent1.getId(),
			parent1.getName(),
			parent1.getVirtualAccount(),
			parent1.getMemberRole(),
			parent1.getRole()
		);

		TokenMemberPrincipal kidFriendPrincipal = new TokenMemberPrincipal(
			kidFriend.getId(),
			kidFriend.getName(),
			kidFriend.getVirtualAccount(),
			kidFriend.getMemberRole(),
			kidFriend.getRole()
		);

		kidAccessToken = jwtTokenProvider.createAccessToken(kidPrincipal);
		parentAccessToken = jwtTokenProvider.createAccessToken(parentPrincipal);
		kidFriendAccessToken = jwtTokenProvider.createAccessToken(kidFriendPrincipal);

		createWalletAccount(me, new BigDecimal("50000"));
		createWalletAccount(parent1, new BigDecimal("100000"));
		createWalletAccount(parent2, new BigDecimal("90000"));
		createWalletAccount(kidFriend, new BigDecimal("30000"));
	}

	@Test
	void getMyWalletTest() throws Exception {
		mvc.perform(get("/api/wallet")
				.header("Authorization", "Bearer " + kidAccessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("내 지갑 잔액 조회에 성공했습니다."))
			.andExpect(jsonPath("$.data").exists())
			.andDo(print());
	}

	@Test
	void getParentsTest() throws Exception {
		mvc.perform(get("/api/parents")
				.header("Authorization", "Bearer " + kidAccessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("부모 리스트 조회에 성공했습니다."))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[*].connectMemberRole", hasItem("PARENT")))
			.andDo(print());
	}

	@Test
	void getKidsTest() throws Exception {
		mvc.perform(get("/api/kids")
				.header("Authorization", "Bearer " + kidAccessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("아이 리스트 조회에 성공했습니다."))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].connectMemberName").value("박친구"))
			.andExpect(jsonPath("$.data[0].connectMemberRole").value("KID"))
			.andDo(print());
	}

	@Test
	void getOtherParentsTest() throws Exception {
		mvc.perform(get("/api/" + kidId + "/parents")
				.header("Authorization", "Bearer " + parentAccessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].connectMemberName").value("이할머니"))
			.andExpect(jsonPath("$.data[0].connectMemberRole").value("PARENT"))
			.andDo(print());
	}

	@Test
	void getOtherParentsFailTest() throws Exception {
		mvc.perform(get("/api/" + kidId + "/parents")
				.header("Authorization", "Bearer " + kidFriendAccessToken))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}

	private String generateAccount() {
		return String.valueOf(accountSeq++);
	}

	private void createWalletAccount(Member member, BigDecimal balance) {
		String accountNumber = generateAccount();
		accountRepository.save(
			Account.builder()
				.name(member.getName() + " 지갑")
				.accountNumber(accountCryptoService.encrypt(accountNumber))
				.accountNumberHash(accountHashService.hash(accountNumber))
				.password(passwordEncoder.encode("1234"))
				.accountType(AccountType.WALLET)
				.balance(balance)
				.member(member)
				.isReward(false)
				.build()
		);
	}
}
