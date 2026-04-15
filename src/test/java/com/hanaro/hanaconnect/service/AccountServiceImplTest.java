package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.dto.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.dto.AccountVerifyRequestDTO;
import com.hanaro.hanaconnect.dto.AccountVerifyResponseDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddRequestDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddResponseDTO;
import com.hanaro.hanaconnect.dto.KidAccountListResponseDTO;
import com.hanaro.hanaconnect.dto.KidWalletDetailResponseDTO;
import com.hanaro.hanaconnect.dto.MyAccountResponseDTO;
import com.hanaro.hanaconnect.dto.TerminatedAccountResponseDTO;
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

	@InjectMocks
	private AccountServiceImpl accountService;

	@Test
	@DisplayName("본인 계좌 등록 성공")
	void linkMyAccount_success() {
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account account = createAccount(10L, "아이 입출금 통장", "11122223333", "encoded", AccountType.FREE, kid);
		LinkedAccount savedLinkedAccount = createLinkedAccount(100L, null, account, kid, LocalDateTime.of(2026, 4, 15, 10, 0));

		AccountLinkRequestDTO request = new AccountLinkRequestDTO();
		request.setAccountNumber("11122223333");
		request.setAccountPassword("1234");

		given(accountRepository.findByAccountNumberAndMemberIdWithLock("11122223333", 1L))
			.willReturn(Optional.of(account));
		given(passwordEncoder.matches("1234", "encoded")).willReturn(true);
		given(linkedAccountRepository.existsByAccountIdAndMemberId(10L, 1L)).willReturn(false);
		given(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).willReturn(savedLinkedAccount);

		AccountLinkResponseDTO result = accountService.linkMyAccount(1L, request);

		assertThat(result.getAccountNumber()).isEqualTo("111-2222-3333");
		assertThat(result.getLinkedAt()).isEqualTo("2026.04.15");
	}

	@Test
	@DisplayName("본인 계좌 등록 시 비밀번호가 다르면 예외가 발생한다")
	void linkMyAccount_fail_wrongPassword() {
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account account = createAccount(10L, "아이 입출금 통장", "11122223333", "encoded", AccountType.FREE, kid);

		AccountLinkRequestDTO request = new AccountLinkRequestDTO();
		request.setAccountNumber("11122223333");
		request.setAccountPassword("9999");

		given(accountRepository.findByAccountNumberAndMemberIdWithLock("11122223333", 1L))
			.willReturn(Optional.of(account));
		given(passwordEncoder.matches("9999", "encoded")).willReturn(false);

		assertThatThrownBy(() -> accountService.linkMyAccount(1L, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("비밀번호를 잘못 입력했습니다.");

		then(linkedAccountRepository).should(never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("본인 계좌 등록 시 이미 연결된 계좌면 예외가 발생한다")
	void linkMyAccount_fail_alreadyLinked() {
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account account = createAccount(10L, "아이 입출금 통장", "11122223333", "encoded", AccountType.FREE, kid);

		AccountLinkRequestDTO request = new AccountLinkRequestDTO();
		request.setAccountNumber("11122223333");
		request.setAccountPassword("1234");

		given(accountRepository.findByAccountNumberAndMemberIdWithLock("11122223333", 1L))
			.willReturn(Optional.of(account));
		given(passwordEncoder.matches("1234", "encoded")).willReturn(true);
		given(linkedAccountRepository.existsByAccountIdAndMemberId(10L, 1L)).willReturn(true);

		assertThatThrownBy(() -> accountService.linkMyAccount(1L, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("이미 등록된 계좌입니다.");
	}

	@Test
	@DisplayName("본인 계좌 등록 중 save 시점에 중복이 발생하면 이미 등록된 계좌 예외를 반환한다")
	void linkMyAccount_fail_duplicateOnSave() {
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account account = createAccount(10L, "아이 입출금 통장", "11122223333", "encoded", AccountType.FREE, kid);

		AccountLinkRequestDTO request = new AccountLinkRequestDTO();
		request.setAccountNumber("11122223333");
		request.setAccountPassword("1234");

		given(accountRepository.findByAccountNumberAndMemberIdWithLock("11122223333", 1L))
			.willReturn(Optional.of(account));
		given(passwordEncoder.matches("1234", "encoded")).willReturn(true);
		given(linkedAccountRepository.existsByAccountIdAndMemberId(10L, 1L)).willReturn(false, true);
		given(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class)))
			.willThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> accountService.linkMyAccount(1L, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("이미 등록된 계좌입니다.");
	}

	@Test
	@DisplayName("본인 계좌 확인 성공")
	void verifyMyAccount_success() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Account account = createAccount(20L, "부모 청약 통장", "44455556666", "encoded", AccountType.SUBSCRIPTION, parent);

		AccountVerifyRequestDTO request = new AccountVerifyRequestDTO();
		request.setAccountNumber("44455556666");

		given(accountRepository.findByAccountNumberAndMemberId("44455556666", 3L))
			.willReturn(Optional.of(account));
		given(linkedAccountRepository.existsByAccountIdAndMemberId(20L, 3L)).willReturn(false);

		AccountVerifyResponseDTO result = accountService.verifyMyAccount(3L, request);

		assertThat(result.getAccountNumber()).isEqualTo("444-5555-6666");
	}

	@Test
	@DisplayName("본인 계좌 확인 시 계좌번호 형식이 잘못되면 예외가 발생한다")
	void verifyMyAccount_fail_invalidAccountNumber() {
		AccountVerifyRequestDTO request = new AccountVerifyRequestDTO();
		request.setAccountNumber("123");

		assertThatThrownBy(() -> accountService.verifyMyAccount(1L, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("계좌 정보를 다시 확인해주세요.");

		then(accountRepository).shouldHaveNoInteractions();
		then(linkedAccountRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("본인 계좌 확인 시 이미 연결된 계좌면 예외가 발생한다")
	void verifyMyAccount_fail_alreadyLinked() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Account account = createAccount(20L, "부모 청약 통장", "44455556666", "encoded", AccountType.SUBSCRIPTION, parent);

		AccountVerifyRequestDTO request = new AccountVerifyRequestDTO();
		request.setAccountNumber("44455556666");

		given(accountRepository.findByAccountNumberAndMemberId("44455556666", 3L))
			.willReturn(Optional.of(account));
		given(linkedAccountRepository.existsByAccountIdAndMemberId(20L, 3L)).willReturn(true);

		assertThatThrownBy(() -> accountService.verifyMyAccount(3L, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("계좌 정보를 다시 확인해주세요.");
	}

	@Test
	@DisplayName("본인 계좌 확인 시 본인 소유 계좌가 아니면 예외가 발생한다")
	void verifyMyAccount_fail_accountNotOwned() {
		AccountVerifyRequestDTO request = new AccountVerifyRequestDTO();
		request.setAccountNumber("44455556666");

		given(accountRepository.findByAccountNumberAndMemberId("44455556666", 3L))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.verifyMyAccount(3L, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("계좌 정보를 다시 확인해주세요.");
	}

	@Test
	@DisplayName("아이 계좌 추가 성공")
	void addKidAccount_success() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account kidAccount = createAccount(30L, "아이 청약 통장", "77788889999", "encoded", AccountType.SUBSCRIPTION, kid);
		LinkedAccount savedLinkedAccount = createLinkedAccount(
			300L,
			"민수 청약",
			kidAccount,
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
		given(accountRepository.findByAccountNumberAndMemberIdWithLock("77788889999", 1L))
			.willReturn(Optional.of(kidAccount));
		given(linkedAccountRepository.existsByAccountIdAndMemberId(30L, 3L)).willReturn(false);
		given(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).willReturn(savedLinkedAccount);

		KidAccountAddResponseDTO result = accountService.addKidAccount(3L, 1L, request);

		ArgumentCaptor<LinkedAccount> captor = ArgumentCaptor.forClass(LinkedAccount.class);
		then(linkedAccountRepository).should().saveAndFlush(captor.capture());
		assertThat(captor.getValue().getNickname()).isEqualTo("민수 청약");

		assertThat(result.getKidName()).isEqualTo("홍길동");
		assertThat(result.getAccountNumber()).isEqualTo("777-8888-9999");
		assertThat(result.getAccountType()).isEqualTo(AccountType.SUBSCRIPTION);
		assertThat(result.getRequestDate()).isEqualTo("2026.04.15");
	}

	@Test
	@DisplayName("아이 계좌 추가 시 부모가 아니면 접근 예외가 발생한다")
	void addKidAccount_fail_nonParent() {
		Member kidRequester = createMember(1L, "홍길동", MemberRole.KID);

		KidAccountAddRequestDTO request = new KidAccountAddRequestDTO();
		request.setAccountNumber("77788889999");
		request.setNickname("민수 청약");

		given(memberRepository.findById(1L)).willReturn(Optional.of(kidRequester));

		assertThatThrownBy(() -> accountService.addKidAccount(1L, 2L, request))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("아이 계좌 추가 시 관계 없는 아이면 예외가 발생한다")
	void addKidAccount_fail_unrelatedKid() {
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
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("계좌 정보를 다시 확인해주세요.");
	}

	@Test
	@DisplayName("아이 계좌 추가 시 이미 연결된 계좌면 예외가 발생한다")
	void addKidAccount_fail_alreadyLinked() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account kidAccount = createAccount(30L, "아이 청약 통장", "77788889999", "encoded", AccountType.SUBSCRIPTION, kid);

		KidAccountAddRequestDTO request = new KidAccountAddRequestDTO();
		request.setAccountNumber("77788889999");
		request.setNickname("민수 청약");

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));
		given(relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(3L, 1L, MemberRole.KID))
			.willReturn(true);
		given(accountRepository.findByAccountNumberAndMemberIdWithLock("77788889999", 1L))
			.willReturn(Optional.of(kidAccount));
		given(linkedAccountRepository.existsByAccountIdAndMemberId(30L, 3L)).willReturn(true);

		assertThatThrownBy(() -> accountService.addKidAccount(3L, 1L, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("이미 등록된 계좌입니다.");
	}

	@Test
	@DisplayName("아이 계좌 추가 중 save 시점에 중복이 발생하면 이미 등록된 계좌 예외를 반환한다")
	void addKidAccount_fail_duplicateOnSave() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account kidAccount = createAccount(30L, "아이 청약 통장", "77788889999", "encoded", AccountType.SUBSCRIPTION, kid);

		KidAccountAddRequestDTO request = new KidAccountAddRequestDTO();
		request.setAccountNumber("77788889999");
		request.setNickname("민수 청약");

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));
		given(relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(3L, 1L, MemberRole.KID))
			.willReturn(true);
		given(accountRepository.findByAccountNumberAndMemberIdWithLock("77788889999", 1L))
			.willReturn(Optional.of(kidAccount));
		given(linkedAccountRepository.existsByAccountIdAndMemberId(30L, 3L)).willReturn(false, true);
		given(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class)))
			.willThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> accountService.addKidAccount(3L, 1L, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("이미 등록된 계좌입니다.");
	}

	@Test
	@DisplayName("내 연결 계좌 조회 시 limit이 있으면 DB 레벨 제한 조회를 사용한다")
	void getMyAccounts_withLimit() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Account account = createAccount(20L, "부모 저축 예금", "22233334444", "encoded", AccountType.DEPOSIT, parent);
		LinkedAccount linkedAccount = createLinkedAccount(200L, null, account, parent, LocalDateTime.of(2026, 4, 15, 12, 0));

		given(linkedAccountRepository.findByMemberIdAndAccount_IsEndFalseOrderByCreatedAtDesc(eq(3L), any(Pageable.class)))
			.willReturn(List.of(linkedAccount));

		List<MyAccountResponseDTO> result = accountService.getMyAccounts(3L, 2);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		then(linkedAccountRepository).should()
			.findByMemberIdAndAccount_IsEndFalseOrderByCreatedAtDesc(eq(3L), captor.capture());

		assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
		assertThat(captor.getValue().getPageSize()).isEqualTo(2);
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("부모 저축 예금");
		assertThat(result.get(0).getCreatedAt()).isEqualTo("2026.04.15");
	}

	@Test
	@DisplayName("내 연결 계좌 조회 시 limit이 없으면 전체 연결 계좌를 조회한다")
	void getMyAccounts_withoutLimit() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Account account = createAccount(20L, "부모 저축 예금", "22233334444", "encoded", AccountType.DEPOSIT, parent);
		LinkedAccount linkedAccount = createLinkedAccount(200L, null, account, parent, LocalDateTime.of(2026, 4, 15, 12, 0));

		given(linkedAccountRepository.findByMemberIdAndAccount_IsEndFalseOrderByCreatedAtDesc(3L))
			.willReturn(List.of(linkedAccount));

		List<MyAccountResponseDTO> result = accountService.getMyAccounts(3L, null);

		then(linkedAccountRepository).should().findByMemberIdAndAccount_IsEndFalseOrderByCreatedAtDesc(3L);
		then(linkedAccountRepository).should(never())
			.findByMemberIdAndAccount_IsEndFalseOrderByCreatedAtDesc(eq(3L), any(Pageable.class));
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getAccountNumber()).isEqualTo("222-3333-4444");
	}

	@Test
	@DisplayName("아이 계좌 목록 조회 시 부모가 아니면 접근 예외가 발생한다")
	void getKidAccounts_fail_nonParent() {
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));

		assertThatThrownBy(() -> accountService.getKidAccounts(1L, 2))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("아이 계좌 목록 조회 시 limit 기반으로 계좌를 매핑한다")
	void getKidAccounts_withLimit() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account kidAccount = createAccount(30L, "아이 적금 통장", "66677778888", "encoded", AccountType.SAVINGS, kid);
		LinkedAccount linkedAccount = createLinkedAccount(300L, "민수 적금", kidAccount, parent, LocalDateTime.of(2026, 4, 15, 13, 0));

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(
			linkedAccountRepository.findByMemberIdAndAccount_Member_MemberRoleAndAccount_IsEndFalseOrderByCreatedAtDesc(
				eq(3L),
				eq(MemberRole.KID),
				any(Pageable.class)
			)
		).willReturn(List.of(linkedAccount));

		List<KidAccountListResponseDTO> result = accountService.getKidAccounts(3L, 2);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getLinkedAccountId()).isEqualTo(300L);
		assertThat(result.get(0).getAccountId()).isEqualTo(30L);
		assertThat(result.get(0).getNickname()).isEqualTo("민수 적금");
		assertThat(result.get(0).getAccountNumber()).isEqualTo("666-7777-8888");
	}

	@Test
	@DisplayName("아이 계좌 목록 조회 시 limit이 없으면 전체 아이 연결 계좌를 조회한다")
	void getKidAccounts_withoutLimit() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);
		Account kidAccount = createAccount(30L, "아이 적금 통장", "66677778888", "encoded", AccountType.SAVINGS, kid);
		LinkedAccount linkedAccount = createLinkedAccount(300L, "민수 적금", kidAccount, parent, LocalDateTime.of(2026, 4, 15, 13, 0));

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(linkedAccountRepository.findByMemberIdAndAccount_Member_MemberRoleAndAccount_IsEndFalseOrderByCreatedAtDesc(
			3L,
			MemberRole.KID
		)).willReturn(List.of(linkedAccount));

		List<KidAccountListResponseDTO> result = accountService.getKidAccounts(3L, null);

		then(linkedAccountRepository).should()
			.findByMemberIdAndAccount_Member_MemberRoleAndAccount_IsEndFalseOrderByCreatedAtDesc(3L, MemberRole.KID);
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getNickname()).isEqualTo("민수 적금");
	}

	@Test
	@DisplayName("부모가 특정 아이의 연결 계좌 지갑 정보를 조회할 수 있다")
	void getKidLinkedAccounts_success() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID, new BigDecimal("7000"));
		Account kidAccount = createAccount(30L, "아이 청약 통장", "77788889999", "encoded", AccountType.SUBSCRIPTION, kid);
		LinkedAccount linkedAccount = createLinkedAccount(300L, "민수 청약", kidAccount, parent, LocalDateTime.of(2026, 4, 15, 14, 0));

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));
		given(relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(3L, 1L, MemberRole.KID))
			.willReturn(true);
		given(linkedAccountRepository.findByMemberIdAndAccount_Member_IdAndAccount_IsEndFalseOrderByCreatedAtDesc(3L, 1L))
			.willReturn(List.of(linkedAccount));

		KidWalletDetailResponseDTO result = accountService.getKidLinkedAccounts(3L, 1L);

		assertThat(result.getKidId()).isEqualTo(1L);
		assertThat(result.getKidName()).isEqualTo("홍길동");
		assertThat(result.getWalletMoney()).isEqualByComparingTo("7000");
		assertThat(result.getAccounts()).hasSize(1);
		assertThat(result.getAccounts().get(0).getNickname()).isEqualTo("민수 청약");
		assertThat(result.getAccounts().get(0).getName()).isEqualTo("아이 청약 통장");
		assertThat(result.getAccounts().get(0).getAccountNumber()).isEqualTo("777-8888-9999");
	}

	@Test
	@DisplayName("특정 아이의 연결 계좌 조회 시 부모가 아니면 접근 예외가 발생한다")
	void getKidLinkedAccounts_fail_nonParent() {
		Member kidRequester = createMember(1L, "홍길동", MemberRole.KID);
		given(memberRepository.findById(1L)).willReturn(Optional.of(kidRequester));

		assertThatThrownBy(() -> accountService.getKidLinkedAccounts(1L, 2L))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("접근 권한이 없습니다.");
	}

	@Test
	@DisplayName("특정 아이의 연결 계좌 조회 시 관계 없는 아이면 예외가 발생한다")
	void getKidLinkedAccounts_fail_unrelatedKid() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);
		Member kid = createMember(1L, "홍길동", MemberRole.KID);

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(1L)).willReturn(Optional.of(kid));
		given(relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(3L, 1L, MemberRole.KID))
			.willReturn(false);

		assertThatThrownBy(() -> accountService.getKidLinkedAccounts(3L, 1L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("계좌 정보를 다시 확인해주세요.");
	}

	@Test
	@DisplayName("특정 아이의 연결 계좌 조회 시 아이 회원이 존재하지 않으면 예외가 발생한다")
	void getKidLinkedAccounts_fail_kidNotFound() {
		Member parent = createMember(3L, "김엄마", MemberRole.PARENT);

		given(memberRepository.findById(3L)).willReturn(Optional.of(parent));
		given(memberRepository.findById(99L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.getKidLinkedAccounts(3L, 99L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("아이 회원이 존재하지 않습니다.");
	}

	private Member createMember(Long id, String name, MemberRole memberRole) {
		return createMember(id, name, memberRole, BigDecimal.ZERO);
	}

	private Member createMember(Long id, String name, MemberRole memberRole, BigDecimal walletMoney) {
		return Member.builder()
			.id(id)
			.name(name)
			.password("encodedPassword")
			.birthday(LocalDate.of(2010, 1, 1))
			.virtualAccount("encryptedAccount")
			.walletMoney(walletMoney)
			.memberRole(memberRole)
			.role(Role.USER)
			.build();
	}

	private Account createAccount(
		Long id,
		String name,
		String accountNumber,
		String password,
		AccountType accountType,
		Member member
	) {
		return Account.builder()
			.id(id)
			.name(name)
			.accountNumber(accountNumber)
			.password(password)
			.accountType(accountType)
			.balance(new BigDecimal("100000"))
			.member(member)
			.isEnd(false)
			.build();
	}

	private LinkedAccount createLinkedAccount(
		Long id,
		String nickname,
		Account account,
		Member member,
		LocalDateTime createdAt
	) {
		LinkedAccount linkedAccount = LinkedAccount.builder()
			.id(id)
			.nickname(nickname)
			.account(account)
			.member(member)
			.build();
		linkedAccount.setCreatedAtForInit(createdAt);
		return linkedAccount;
	}
}
