package com.hanaro.hanaconnect.service;

import java.util.List;
import java.time.format.DateTimeFormatter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.util.AccountCryptoService;
import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.dto.account.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.account.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.dto.account.AccountVerifyRequestDTO;
import com.hanaro.hanaconnect.dto.account.AccountVerifyResponseDTO;
import com.hanaro.hanaconnect.dto.account.KidAccountAddRequestDTO;
import com.hanaro.hanaconnect.dto.account.KidAccountAddResponseDTO;
import com.hanaro.hanaconnect.dto.account.KidAccountListResponseDTO;
import com.hanaro.hanaconnect.dto.account.KidLinkedAccountResponseDTO;
import com.hanaro.hanaconnect.dto.account.KidWalletDetailResponseDTO;
import com.hanaro.hanaconnect.dto.account.MyAccountResponseDTO;
import com.hanaro.hanaconnect.dto.account.RewardAccountResponseDTO;
import com.hanaro.hanaconnect.dto.account.TerminatedAccountResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

import jakarta.persistence.EntityNotFoundException;
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

	private final AccountHashService accountHashService;
	private final AccountCryptoService accountCryptoService;

	@Override
	public AccountLinkResponseDTO linkMyAccount(Long memberId, AccountLinkRequestDTO request) {
		String normalizedAccountNumber = AccountNumberFormatter.normalize(request.getAccountNumber());

		validateAccountNumber(normalizedAccountNumber);

		String accountNumberHash = accountHashService.hash(normalizedAccountNumber);

		Account account = accountRepository.findByAccountNumberHashAndMemberId(accountNumberHash, memberId)
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

		String decryptedAccountNumber = accountCryptoService.decrypt(account.getAccountNumber());

		return new AccountLinkResponseDTO(
			AccountNumberFormatter.format(decryptedAccountNumber),
			savedLinkedAccount.getCreatedAt().format(LINKED_AT_FORMATTER)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public AccountVerifyResponseDTO verifyMyAccount(Long memberId, AccountVerifyRequestDTO request) {
		String normalizedAccountNumber = AccountNumberFormatter.normalize(request.getAccountNumber());

		validateAccountNumber(normalizedAccountNumber);

		String accountNumberHash = accountHashService.hash(normalizedAccountNumber);

		Account account = accountRepository.findByAccountNumberHashAndMemberId(accountNumberHash, memberId)
			.orElseThrow(() -> new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE));

		if (linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), memberId)) {
			throw new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE);
		}

		String decryptedAccountNumber = accountCryptoService.decrypt(account.getAccountNumber());

		return new AccountVerifyResponseDTO(
			AccountNumberFormatter.format(decryptedAccountNumber),
			account.getAccountType()
		);
	}

	@Override
	public KidAccountAddResponseDTO addKidAccount(Long memberId, Long kidId, KidAccountAddRequestDTO request) {
		validateParentKidRelation(memberId, kidId);

		String normalizedAccountNumber = AccountNumberFormatter.normalize(request.getAccountNumber());
		validateAccountNumber(normalizedAccountNumber);

		String accountNumberHash = accountHashService.hash(normalizedAccountNumber);

		Account account = accountRepository.findByAccountNumberHashAndMemberId(accountNumberHash, kidId)
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

		String decryptedAccountNumber = accountCryptoService.decrypt(account.getAccountNumber());

		return new KidAccountAddResponseDTO(
			account.getMember().getName(),
			AccountNumberFormatter.format(decryptedAccountNumber),
			account.getAccountType(),
			savedLinkedAccount.getCreatedAt().format(LINKED_AT_FORMATTER)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MyAccountResponseDTO> getMyAccounts(Long memberId, Integer limit) {
		List<LinkedAccount> linkedAccounts = getLinkedAccounts(memberId, limit);

		return linkedAccounts.stream()
			.map(linkedAccount -> {
				Account account = linkedAccount.getAccount();
				String decryptedAccountNumber = accountCryptoService.decrypt(account.getAccountNumber());

				return MyAccountResponseDTO.builder()
					.accountId(linkedAccount.getId())
					.name(account.getName())
					.accountNumber(AccountNumberFormatter.format(decryptedAccountNumber))
					.balance(account.getBalance())
					.accountType(account.getAccountType())
					.createdAt(linkedAccount.getCreatedAt().format(LINKED_AT_FORMATTER))
					.build();
			})
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<KidAccountListResponseDTO> getKidAccounts(Long memberId, Integer limit) {
		validateParentRole(memberId);

		List<LinkedAccount> linkedAccounts = getLinkedKidAccounts(memberId, limit);

		return linkedAccounts.stream()
			.map(linkedAccount -> {
				String decryptedAccountNumber =
					accountCryptoService.decrypt(linkedAccount.getAccount().getAccountNumber());

				return KidAccountListResponseDTO.builder()
					.linkedAccountId(linkedAccount.getId())
					.accountId(linkedAccount.getAccount().getId())
					.nickname(linkedAccount.getNickname())
					.accountNumber(AccountNumberFormatter.format(decryptedAccountNumber))
					.build();
			})
			.toList();
	}

	private List<LinkedAccount> getLinkedAccounts(Long memberId, Integer limit) {
		if (limit != null && limit > 0) {
			Pageable pageable = PageRequest.of(0, limit);
			return linkedAccountRepository.findByMemberIdAndAccount_IsEndFalseOrderByCreatedAtDesc(memberId, pageable);
		}

		return linkedAccountRepository.findByMemberIdAndAccount_IsEndFalseOrderByCreatedAtDesc(memberId);
	}

	private List<LinkedAccount> getLinkedKidAccounts(Long memberId, Integer limit) {
		if (limit != null && limit > 0) {
			Pageable pageable = PageRequest.of(0, limit);
			return linkedAccountRepository
				.findByMemberIdAndAccount_Member_MemberRoleAndAccount_IsEndFalseOrderByCreatedAtDesc(
					memberId,
					MemberRole.KID,
					pageable
				);
		}

		return linkedAccountRepository.findByMemberIdAndAccount_Member_MemberRoleAndAccount_IsEndFalseOrderByCreatedAtDesc(
			memberId,
			MemberRole.KID
		);
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
			.map(account -> TerminatedAccountResponseDTO.from(
				account,
				accountCryptoService.decrypt(account.getAccountNumber())
			))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public RewardAccountResponseDTO getRewardAccount(Long memberId) {
		LinkedAccount linkedAccount = linkedAccountRepository
			.findByMemberIdAndAccount_IsRewardTrue(memberId)
			.orElseThrow(() -> new EntityNotFoundException("리워드 계좌를 찾을 수 없습니다."));

		String decryptedAccountNumber =
			accountCryptoService.decrypt(linkedAccount.getAccount().getAccountNumber());

		return RewardAccountResponseDTO.from(linkedAccount, decryptedAccountNumber);
	}

	@Override
	@Transactional
	public RewardAccountResponseDTO updateRewardAccount(Long memberId, Long linkedAccountId) {
		LinkedAccount linkedAccount = linkedAccountRepository.findById(linkedAccountId)
			.orElseThrow(() -> new EntityNotFoundException("계좌를 찾을 수 없습니다."));

		if (!linkedAccount.getMember().getId().equals(memberId)) {
			throw new AccessDeniedException("해당 계좌에 접근할 수 없습니다.");
		}

		Account target = linkedAccount.getAccount();

		if (target.getAccountType() != AccountType.PENSION) {
			throw new IllegalArgumentException("연금 계좌만 리워드 계좌로 설정할 수 있습니다.");
		}

		if (Boolean.TRUE.equals(target.getIsReward())) {
			throw new IllegalStateException("이미 리워드 계좌로 설정되어 있습니다.");
		}

		accountRepository.findByMemberIdAndIsRewardTrue(memberId)
			.ifPresent(prev -> accountRepository.updateIsReward(prev.getId(), false));

		accountRepository.updateIsReward(target.getId(), true);

		String decryptedAccountNumber =
			accountCryptoService.decrypt(linkedAccount.getAccount().getAccountNumber());

		return RewardAccountResponseDTO.from(linkedAccount, decryptedAccountNumber);
	}

	@Override
	@Transactional(readOnly = true)
	public KidWalletDetailResponseDTO getKidLinkedAccounts(Long parentId, Long kidId) {
		validateParentKidRelation(parentId, kidId);

		Member kid = memberRepository.findById(kidId)
			.orElseThrow(() -> new IllegalArgumentException("아이 회원이 존재하지 않습니다."));

		Account kidWalletAccount = accountRepository
			.findByMemberIdAndAccountType(kidId, AccountType.WALLET)
			.orElseThrow(() -> new IllegalArgumentException("아이 지갑 계좌가 없습니다."));

		List<LinkedAccount> linkedAccounts =
			linkedAccountRepository.findKidLinkedAccounts(parentId, kidId);

		List<KidLinkedAccountResponseDTO> accountDTOs = linkedAccounts.stream()
			.map(linkedAccount -> {
				String decryptedAccountNumber =
					accountCryptoService.decrypt(linkedAccount.getAccount().getAccountNumber());

				return KidLinkedAccountResponseDTO.builder()
					.linkedAccountId(linkedAccount.getId())
					.accountId(linkedAccount.getAccount().getId())
					.nickname(linkedAccount.getNickname())
					.name(linkedAccount.getAccount().getName())
					.accountNumber(AccountNumberFormatter.format(decryptedAccountNumber))
					.balance(linkedAccount.getAccount().getBalance())
					.accountType(linkedAccount.getAccount().getAccountType())
					.build();
			})
			.toList();

		return KidWalletDetailResponseDTO.builder()
			.kidId(kid.getId())
			.kidName(kid.getName())
			.walletMoney(kidWalletAccount.getBalance())
			.accounts(accountDTOs)
			.build();
	}

}
