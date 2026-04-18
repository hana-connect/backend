package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.dto.account.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.account.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.dto.account.AccountVerifyRequestDTO;
import com.hanaro.hanaconnect.dto.account.AccountVerifyResponseDTO;
import com.hanaro.hanaconnect.dto.account.KidAccountAddRequestDTO;
import com.hanaro.hanaconnect.dto.account.KidAccountAddResponseDTO;
import com.hanaro.hanaconnect.dto.account.KidWalletDetailResponseDTO;
import com.hanaro.hanaconnect.dto.account.MyAccountResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private LinkedAccountRepository linkedAccountRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RelationRepository relationRepository;

	@Mock
	private AccountHashService accountHashService;

	@Mock
	private AccountCryptoService accountCryptoService;

	@Mock
	private AssetAIService assetAIService;

	@InjectMocks
	private AccountServiceImpl accountService;

	@Test
	@DisplayName("본인 계좌 등록 성공")
	void linkMyAccount_success() {
		Member kid = createMember(1L, "홍길동", MemberRole.KID);

		Account account = createAccount(
			10L,
			"아이 입출금 통장",
			"encrypted-11122223333",
			"encoded",
			AccountType.FREE,
			kid
		);

		LinkedAccount saved = createLinkedAccount(
			100L, null, account, kid,
			LocalDateTime.of(2026, 4, 15, 10, 0)
		);

		AccountLinkRequestDTO request = new AccountLinkRequestDTO();
		request.setAccountNumber("11122223333");
		request.setAccountPassword("1234");

		given(accountHashService.hash("11122223333"))
			.willReturn("hashed-11122223333");

		given(accountRepository.findByAccountNumberHashAndMemberId("hashed-11122223333", 1L))
			.willReturn(Optional.of(account));

		given(passwordEncoder.matches("1234", "encoded")).willReturn(true);
		given(linkedAccountRepository.existsByAccountIdAndMemberId(10L, 1L)).willReturn(false);
		given(linkedAccountRepository.saveAndFlush(any())).willReturn(saved);

		given(accountCryptoService.decrypt("encrypted-11122223333"))
			.willReturn("11122223333");

		AccountLinkResponseDTO result = accountService.linkMyAccount(1L, request);

		assertThat(result.getAccountNumber()).isEqualTo("111-2222-3333");
		assertThat(result.getLinkedAt()).isEqualTo("2026.04.15");

		verify(assetAIService, times(1)).clearRecommendationCache(1L);
	}

	@Test
	@DisplayName("본인 계좌 확인 성공")
	void verifyMyAccount_success() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);

		Account account = createAccount(
			20L,
			"부모 통장",
			"encrypted-44455556666",
			"encoded",
			AccountType.SUBSCRIPTION,
			parent
		);

		AccountVerifyRequestDTO request = new AccountVerifyRequestDTO();
		request.setAccountNumber("44455556666");

		given(accountHashService.hash("44455556666"))
			.willReturn("hashed-44455556666");

		given(accountRepository.findByAccountNumberHashAndMemberId("hashed-44455556666", 3L))
			.willReturn(Optional.of(account));

		given(linkedAccountRepository.existsByAccountIdAndMemberId(20L, 3L))
			.willReturn(false);

		given(accountCryptoService.decrypt("encrypted-44455556666"))
			.willReturn("44455556666");

		AccountVerifyResponseDTO result = accountService.verifyMyAccount(3L, request);

		assertThat(result.getAccountNumber()).isEqualTo("444-5555-6666");
	}

	@Test
	@DisplayName("아이 계좌 추가 성공")
	void addKidAccount_success() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);

		Account account = createAccount(
			30L,
			"아이 통장",
			"encrypted-77788889999",
			"encoded",
			AccountType.SUBSCRIPTION,
			kid
		);

		LinkedAccount saved = createLinkedAccount(
			300L,
			"민수 청약",
			account,
			parent,
			LocalDateTime.of(2026, 4, 15, 11, 0)
		);

		KidAccountAddRequestDTO request = new KidAccountAddRequestDTO();
		request.setAccountNumber("77788889999");
		request.setNickname("  민수 청약  ");

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));
		given(relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(3L, 1L, MemberRole.KID))
			.willReturn(true);

		given(accountHashService.hash("77788889999"))
			.willReturn("hashed-77788889999");

		given(accountRepository.findByAccountNumberHashAndMemberId("hashed-77788889999", 1L))
			.willReturn(Optional.of(account));

		given(linkedAccountRepository.existsByAccountIdAndMemberId(30L, 3L))
			.willReturn(false);

		given(linkedAccountRepository.saveAndFlush(any()))
			.willReturn(saved);

		given(accountCryptoService.decrypt("encrypted-77788889999"))
			.willReturn("77788889999");

		KidAccountAddResponseDTO result = accountService.addKidAccount(3L, 1L, request);

		assertThat(result.getAccountNumber()).isEqualTo("777-8888-9999");
		assertThat(result.getKidName()).isEqualTo("홍길동");
	}

	@Test
	@DisplayName("내 계좌 조회 (decrypt 적용)")
	void getMyAccounts_success() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);

		Account account = createAccount(
			20L,
			"부모 계좌",
			"encrypted-22233334444",
			"encoded",
			AccountType.DEPOSIT,
			parent
		);

		LinkedAccount linked = createLinkedAccount(
			200L,
			null,
			account,
			parent,
			LocalDateTime.of(2026, 4, 15, 12, 0)
		);

		given(linkedAccountRepository.findByMemberIdAndAccount_Member_IdAndAccount_IsEndFalseOrderByCreatedAtDesc(3L, 3L))
			.willReturn(List.of(linked));

		given(accountCryptoService.decrypt("encrypted-22233334444"))
			.willReturn("22233334444");

		List<MyAccountResponseDTO> result = accountService.getMyAccounts(3L, null);

		assertThat(result.get(0).getAccountNumber()).isEqualTo("222-3333-4444");
	}

	@Test
	@DisplayName("아이 지갑 조회 성공")
	void getKidLinkedAccounts_success() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);

		Account kidWalletAccount = createAccount(
			99L,
			"아이 지갑",
			"encrypted-11122220000",
			"encoded",
			AccountType.WALLET,
			kid
		);

		Account account = createAccount(
			30L,
			"아이 계좌",
			"encrypted-77788889999",
			"encoded",
			AccountType.SUBSCRIPTION,
			kid
		);

		LinkedAccount linked = createLinkedAccount(
			300L,
			"민수 청약",
			account,
			parent,
			LocalDateTime.of(2026, 4, 15, 14, 0)
		);

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));
		given(relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(3L, 1L, MemberRole.KID))
			.willReturn(true);

		given(accountRepository.findByMemberIdAndAccountType(1L, AccountType.WALLET))
			.willReturn(Optional.of(kidWalletAccount));

		given(linkedAccountRepository.findKidLinkedAccounts(3L, 1L))
			.willReturn(List.of(linked));

		given(accountCryptoService.decrypt("encrypted-77788889999"))
			.willReturn("77788889999");

		KidWalletDetailResponseDTO result = accountService.getKidLinkedAccounts(3L, 1L);

		assertThat(result.getAccounts().get(0).getAccountNumber())
			.isEqualTo("777-8888-9999");
		assertThat(result.getWalletMoney()).isEqualByComparingTo("100000");
	}

	@Test
	@DisplayName("본인 계좌 등록 실패 - 계좌가 없으면 예외")
	void linkMyAccount_fail_accountNotFound() {
		AccountLinkRequestDTO request = new AccountLinkRequestDTO();
		request.setAccountNumber("11122223333");
		request.setAccountPassword("1234");

		given(accountHashService.hash("11122223333"))
			.willReturn("hashed-11122223333");
		given(accountRepository.findByAccountNumberHashAndMemberId("hashed-11122223333", 1L))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.linkMyAccount(1L, request))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("본인 계좌 등록 실패 - 계좌 비밀번호 불일치")
	void linkMyAccount_fail_wrongPassword() {
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account account = createAccount(
			10L, "아이 입출금 통장", "encrypted-11122223333", "encoded", AccountType.FREE, kid
		);

		AccountLinkRequestDTO request = new AccountLinkRequestDTO();
		request.setAccountNumber("11122223333");
		request.setAccountPassword("9999");

		given(accountHashService.hash("11122223333"))
			.willReturn("hashed-11122223333");
		given(accountRepository.findByAccountNumberHashAndMemberId("hashed-11122223333", 1L))
			.willReturn(Optional.of(account));
		given(passwordEncoder.matches("9999", "encoded"))
			.willReturn(false);

		assertThatThrownBy(() -> accountService.linkMyAccount(1L, request))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("본인 계좌 등록 실패 - 이미 연결된 계좌")
	void linkMyAccount_fail_alreadyLinked() {
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account account = createAccount(
			10L, "아이 입출금 통장", "encrypted-11122223333", "encoded", AccountType.FREE, kid
		);

		AccountLinkRequestDTO request = new AccountLinkRequestDTO();
		request.setAccountNumber("11122223333");
		request.setAccountPassword("1234");

		given(accountHashService.hash("11122223333"))
			.willReturn("hashed-11122223333");
		given(accountRepository.findByAccountNumberHashAndMemberId("hashed-11122223333", 1L))
			.willReturn(Optional.of(account));
		given(passwordEncoder.matches("1234", "encoded"))
			.willReturn(true);
		given(linkedAccountRepository.existsByAccountIdAndMemberId(10L, 1L))
			.willReturn(true);

		assertThatThrownBy(() -> accountService.linkMyAccount(1L, request))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("본인 계좌 확인 실패 - 계좌가 없으면 예외")
	void verifyMyAccount_fail_accountNotFound() {
		AccountVerifyRequestDTO request = new AccountVerifyRequestDTO();
		request.setAccountNumber("44455556666");

		given(accountHashService.hash("44455556666"))
			.willReturn("hashed-44455556666");
		given(accountRepository.findByAccountNumberHashAndMemberId("hashed-44455556666", 3L))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.verifyMyAccount(3L, request))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("본인 계좌 확인 실패 - 이미 연결된 계좌")
	void verifyMyAccount_fail_alreadyLinked() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Account account = createAccount(
			20L, "부모 통장", "encrypted-44455556666", "encoded", AccountType.SUBSCRIPTION, parent
		);

		AccountVerifyRequestDTO request = new AccountVerifyRequestDTO();
		request.setAccountNumber("44455556666");

		given(accountHashService.hash("44455556666"))
			.willReturn("hashed-44455556666");
		given(accountRepository.findByAccountNumberHashAndMemberId("hashed-44455556666", 3L))
			.willReturn(Optional.of(account));
		given(linkedAccountRepository.existsByAccountIdAndMemberId(20L, 3L))
			.willReturn(true);

		assertThatThrownBy(() -> accountService.verifyMyAccount(3L, request))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("아이 계좌 추가 실패 - 부모가 없으면 예외")
	void addKidAccount_fail_parentNotFound() {
		KidAccountAddRequestDTO request = new KidAccountAddRequestDTO();
		request.setAccountNumber("77788889999");
		request.setNickname("민수 청약");

		given(memberRepository.findById(3L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.addKidAccount(3L, 1L, request))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("아이 계좌 추가 실패 - 부모와 아이 관계가 없으면 예외")
	void addKidAccount_fail_noRelation() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);

		KidAccountAddRequestDTO request = new KidAccountAddRequestDTO();
		request.setAccountNumber("77788889999");
		request.setNickname("민수 청약");

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));
		given(relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(3L, 1L, MemberRole.KID))
			.willReturn(false);

		assertThatThrownBy(() -> accountService.addKidAccount(3L, 1L, request))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("아이 계좌 추가 실패 - 이미 연결된 계좌")
	void addKidAccount_fail_alreadyLinked() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);

		Account account = createAccount(
			30L, "아이 통장", "encrypted-77788889999", "encoded", AccountType.SUBSCRIPTION, kid
		);

		KidAccountAddRequestDTO request = new KidAccountAddRequestDTO();
		request.setAccountNumber("77788889999");
		request.setNickname("민수 청약");

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));
		given(relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(3L, 1L, MemberRole.KID))
			.willReturn(true);
		given(accountHashService.hash("77788889999"))
			.willReturn("hashed-77788889999");
		given(accountRepository.findByAccountNumberHashAndMemberId("hashed-77788889999", 1L))
			.willReturn(Optional.of(account));
		given(linkedAccountRepository.existsByAccountIdAndMemberId(30L, 3L))
			.willReturn(true);

		assertThatThrownBy(() -> accountService.addKidAccount(3L, 1L, request))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("아이 지갑 조회 실패 - 관계 없으면 예외")
	void getKidLinkedAccounts_fail_noRelation() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID, new BigDecimal("7000"));

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));
		given(relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(3L, 1L, MemberRole.KID))
			.willReturn(false);

		assertThatThrownBy(() -> accountService.getKidLinkedAccounts(3L, 1L))
			.isInstanceOf(IllegalArgumentException.class);
	}

	// ===== helper =====

	private Member createMember(Long id, String name, MemberRole role) {
		return createMember(id, name, role, BigDecimal.ZERO);
	}

	private Member createMember(Long id, String name, MemberRole role, BigDecimal walletMoney) {
		return Member.builder()
			.id(id)
			.name(name)
			.password("encoded")
			.birthday(LocalDate.of(2010, 1, 1))
			.virtualAccount("encrypted")
			.walletMoney(walletMoney)
			.memberRole(role)
			.role(Role.USER)
			.build();
	}

	private Account createAccount(Long id, String name, String accountNumber, String password,
		AccountType type, Member member) {
		return Account.builder()
			.id(id)
			.name(name)
			.accountNumber(accountNumber)
			.password(password)
			.accountType(type)
			.balance(new BigDecimal("100000"))
			.member(member)
			.isEnd(false)
			.build();
	}

	private LinkedAccount createLinkedAccount(Long id, String nickname,
		Account account, Member member,
		LocalDateTime createdAt) {
		LinkedAccount linked = LinkedAccount.builder()
			.id(id)
			.nickname(nickname)
			.account(account)
			.member(member)
			.build();
		linked.setCreatedAtForInit(createdAt);
		return linked;
	}
}
