package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hanaro.hanaconnect.common.config.TestSecurityConfig;
import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.House;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.service.AccountHashService;

@Import({AccountCryptoService.class, TestSecurityConfig.class, AccountHashService.class})
class HouseRepositoryTest extends BaseRepositoryTest {

	@Autowired
	private HouseRepository houseRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private AccountCryptoService accountCryptoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccountHashService accountHashService;

	private static long virtualAccountSeq = 10000000000L;
	private static long accountNumberSeq = 20000000000L;

	private Member createMember(String name, MemberRole memberRole) {
		return memberRepository.save(
			Member.builder()
				.name(name)
				.password(passwordEncoder.encode("123456"))
				.birthday(LocalDate.of(2010, 1, 1))
				.virtualAccount(generateVirtualAccount())
				.memberRole(memberRole)
				.role(Role.USER)
				.build()
		);
	}

	private Account createAccount(String name, AccountType accountType, Member member) {
		String rawAccountNumber = generateAccountNumber();

		return accountRepository.save(
			Account.builder()
				.name(name)
				.accountNumber(accountCryptoService.encrypt(rawAccountNumber))
				.accountNumberHash(accountHashService.hash(rawAccountNumber))
				.password(passwordEncoder.encode("1234"))
				.accountType(accountType)
				.balance(new BigDecimal("100000"))
				.member(member)
				.build()
		);
	}

	private House createHouse(Member kid, Account account) {
		return houseRepository.save(
			House.builder()
				.member(kid)
				.account(account)
				.level(3)
				.totalCount(28)
				.monthlyPayment(new BigDecimal("200000"))
				.startDate(LocalDate.of(2024, 1, 25))
				.build()
		);
	}

	@Test
	@DisplayName("memberId로 house를 조회할 수 있다")
	void findByMemberIdTest() {
		Member kid = createMember("김청약", MemberRole.KID);
		Account account = createAccount("김청약 주택청약", AccountType.SUBSCRIPTION, kid);
		House savedHouse = createHouse(kid, account);

		Optional<House> result = houseRepository.findByMemberId(kid.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(savedHouse.getId());
		assertThat(result.get().getMember().getId()).isEqualTo(kid.getId());
		assertThat(result.get().getTotalCount()).isEqualTo(28);
		assertThat(result.get().getMonthlyPayment()).isEqualByComparingTo("200000");
	}

	@Test
	@DisplayName("house가 없는 memberId로 조회하면 empty를 반환한다")
	void findByMemberIdEmptyTest() {
		Member kid = createMember("홍길동", MemberRole.KID);

		Optional<House> result = houseRepository.findByMemberId(kid.getId());

		assertThat(result).isEmpty();
	}

	private String generateVirtualAccount() {
		return String.valueOf(virtualAccountSeq++);
	}

	private String generateAccountNumber() {
		return String.valueOf(accountNumberSeq++);
	}
}
