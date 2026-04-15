package com.hanaro.hanaconnect.service;

import java.util.List;
import java.time.format.DateTimeFormatter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.dto.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.dto.AccountVerifyRequestDTO;
import com.hanaro.hanaconnect.dto.AccountVerifyResponseDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddRequestDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddResponseDTO;
import com.hanaro.hanaconnect.dto.KidAccountListResponseDTO;
import com.hanaro.hanaconnect.dto.KidLinkedAccountResponseDTO;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

	private static final DateTimeFormatter LINKED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
	private static final String INVALID_ACCOUNT_MESSAGE = "계좌 정보를 다시 확인해주세요.";
	private static final String INVALID_ACCOUNT_PASSWORD_MESSAGE = "비밀번호를 잘못 입력했습니다.";
	private static final String ALREADY_LINKED_ACCOUNT_MESSAGE = "이미 등록된 계좌입니다.";

	private final AccountRepository accountRepository;
	private final LinkedAccountRepository linkedAccountRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final RelationRepository relationRepository;

	@Override
	public AccountLinkResponseDTO linkMyAccount(Long memberId, AccountLinkRequestDTO request) {
		String normalizedAccountNumber = AccountNumberFormatter.normalize(request.getAccountNumber());

		validateAccountNumber(normalizedAccountNumber);

		Account account = accountRepository.findByAccountNumberAndMemberIdWithLock(normalizedAccountNumber, memberId)
			.orElseThrow(() -> new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE));

		if (!passwordEncoder.matches(request.getAccountPassword(), account.getPassword())) {
			throw new IllegalArgumentException(INVALID_ACCOUNT_PASSWORD_MESSAGE);
		}

		if (linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), memberId)) {
			throw new IllegalArgumentException(ALREADY_LINKED_ACCOUNT_MESSAGE);
		}

		LinkedAccount linkedAccount = LinkedAccount.builder()
			.account(account)
			.member(account.getMember())
			.build();

		LinkedAccount savedLinkedAccount;
		try {
			savedLinkedAccount = linkedAccountRepository.saveAndFlush(linkedAccount);
		} catch (DataIntegrityViolationException e) {
			if (linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), memberId)) {
				throw new IllegalArgumentException(ALREADY_LINKED_ACCOUNT_MESSAGE);
			}
			throw e;
		}

		return new AccountLinkResponseDTO(
			AccountNumberFormatter.format(account.getAccountNumber()),
			savedLinkedAccount.getCreatedAt().format(LINKED_AT_FORMATTER)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public AccountVerifyResponseDTO verifyMyAccount(Long memberId, AccountVerifyRequestDTO request) {
		String normalizedAccountNumber = AccountNumberFormatter.normalize(request.getAccountNumber());

		validateAccountNumber(normalizedAccountNumber);

		Account account = accountRepository.findByAccountNumberAndMemberId(normalizedAccountNumber, memberId)
			.orElseThrow(() -> new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE));

		if (linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), memberId)) {
			throw new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE);
		}

		return new AccountVerifyResponseDTO(
			AccountNumberFormatter.format(account.getAccountNumber())
		);
	}

	@Override
	public KidAccountAddResponseDTO addKidAccount(Long memberId, Long kidId, KidAccountAddRequestDTO request) {
		validateParentKidRelation(memberId, kidId);

		String normalizedAccountNumber = AccountNumberFormatter.normalize(request.getAccountNumber());
		validateAccountNumber(normalizedAccountNumber);

		Account account = accountRepository.findByAccountNumberAndMemberIdWithLock(normalizedAccountNumber, kidId)
			.orElseThrow(() -> new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE));

		if (linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), memberId)) {
			throw new IllegalArgumentException(ALREADY_LINKED_ACCOUNT_MESSAGE);
		}

		Member parentMember = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE));

		LinkedAccount linkedAccount = LinkedAccount.builder()
			.nickname(request.getNickname().trim())
			.account(account)
			.member(parentMember)
			.build();

		LinkedAccount savedLinkedAccount;
		try {
			savedLinkedAccount = linkedAccountRepository.saveAndFlush(linkedAccount);
		} catch (DataIntegrityViolationException e) {
			if (linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), memberId)) {
				throw new IllegalArgumentException(ALREADY_LINKED_ACCOUNT_MESSAGE);
			}
			throw e;
		}

		return new KidAccountAddResponseDTO(
			account.getMember().getName(),
			AccountNumberFormatter.format(account.getAccountNumber()),
			account.getAccountType(),
			savedLinkedAccount.getCreatedAt().format(LINKED_AT_FORMATTER)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MyAccountResponseDTO> getMyAccounts(Long memberId, Integer limit) {
		List<Account> accounts = accountRepository.findByMemberIdAndIsEndFalseOrderByCreatedAtDesc(memberId);

		if (limit != null && limit > 0 && accounts.size() > limit) {
			accounts = accounts.subList(0, limit);
		}

		return accounts.stream()
			.map(account -> MyAccountResponseDTO.builder()
				.accountId(account.getId())
				.name(account.getName())
				.accountNumber(AccountNumberFormatter.format(account.getAccountNumber()))
				.balance(account.getBalance())
				.accountType(account.getAccountType())
				.createdAt(account.getCreatedAt().format(LINKED_AT_FORMATTER))
				.build())
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<KidAccountListResponseDTO> getKidAccounts(Long memberId, Integer limit) {
		validateParentRole(memberId);

		List<LinkedAccount> linkedAccounts =
			linkedAccountRepository.findByMemberIdAndAccount_Member_MemberRoleAndAccount_IsEndFalseOrderByCreatedAtDesc(
				memberId,
				MemberRole.KID
			);

		if (limit != null && limit > 0 && linkedAccounts.size() > limit) {
			linkedAccounts = linkedAccounts.subList(0, limit);
		}

		return linkedAccounts.stream()
			.map(linkedAccount -> KidAccountListResponseDTO.builder()
				.linkedAccountId(linkedAccount.getId())
				.accountId(linkedAccount.getAccount().getId())
				.nickname(linkedAccount.getNickname())
				.accountNumber(AccountNumberFormatter.format(linkedAccount.getAccount().getAccountNumber()))
				.build())
			.toList();
	}

	private void validateAccountNumber(String accountNumber) {
		if (accountNumber == null || !accountNumber.matches("^\\d{11}$")) {
			throw new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE);
		}
	}

	private void validateParentKidRelation(Long memberId, Long kidId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE));

		if (member.getMemberRole() != MemberRole.PARENT) {
			throw new AccessDeniedException("접근 권한이 없습니다.");
		}

		Member kid = memberRepository.findById(kidId)
			.orElseThrow(() -> new IllegalArgumentException("아이 회원이 존재하지 않습니다."));

		if (kid.getMemberRole() != MemberRole.KID) {
			throw new IllegalArgumentException("아이 회원이 존재하지 않습니다.");
		}

		boolean isLinkedKid = relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
			memberId,
			kidId,
			MemberRole.KID
		);

		if (!isLinkedKid) {
			throw new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE);
		}
	}

	private void validateParentRole(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE));

		if (member.getMemberRole() != MemberRole.PARENT) {
			throw new AccessDeniedException("접근 권한이 없습니다.");
		}
	}

	@Override
	public List<TerminatedAccountResponseDTO> getTerminatedSavings(Long memberId) {
		return accountRepository.findByMemberIdAndAccountTypeAndIsEndTrueOrderByIdAsc(
				memberId,
				AccountType.SAVINGS
			).stream()
			.map(TerminatedAccountResponseDTO::from)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public KidWalletDetailResponseDTO getKidLinkedAccounts(Long parentId, Long kidId) {
		validateParentKidRelation(parentId, kidId);

		Member kid = memberRepository.findById(kidId)
			.orElseThrow(() -> new IllegalArgumentException("아이 회원이 존재하지 않습니다."));

		List<LinkedAccount> linkedAccounts = linkedAccountRepository
			.findByMemberIdAndAccount_Member_IdAndAccount_IsEndFalseOrderByCreatedAtDesc(parentId, kidId);

		List<KidLinkedAccountResponseDTO> accountDTOs = linkedAccounts.stream()
			.map(linkedAccount -> KidLinkedAccountResponseDTO.builder()
				.linkedAccountId(linkedAccount.getId())
				.accountId(linkedAccount.getAccount().getId())
				.nickname(linkedAccount.getNickname())
				.name(linkedAccount.getAccount().getName())
				.accountNumber(AccountNumberFormatter.format(linkedAccount.getAccount().getAccountNumber()))
				.balance(linkedAccount.getAccount().getBalance())
				.accountType(linkedAccount.getAccount().getAccountType())
				.build())
			.toList();

		return KidWalletDetailResponseDTO.builder()
			.kidId(kid.getId())
			.kidName(kid.getName())
			.walletMoney(kid.getWalletMoney())
			.accounts(accountDTOs)
			.build();
	}

}
