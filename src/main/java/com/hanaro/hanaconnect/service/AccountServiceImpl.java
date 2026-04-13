package com.hanaro.hanaconnect.service;

import java.time.format.DateTimeFormatter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.dto.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;

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
	private final PasswordEncoder passwordEncoder;

	@Override
	public AccountLinkResponseDTO linkMyAccount(Long memberId, AccountLinkRequestDTO request) {
		String normalizedAccountNumber = AccountNumberFormatter.normalize(request.getAccountNumber());

		validateAccountNumber(normalizedAccountNumber);

		Account account = accountRepository.findByAccountNumberAndMemberId(normalizedAccountNumber, memberId)
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
			savedLinkedAccount = linkedAccountRepository.save(linkedAccount);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException(ALREADY_LINKED_ACCOUNT_MESSAGE);
		}

		return new AccountLinkResponseDTO(
			AccountNumberFormatter.format(account.getAccountNumber()),
			savedLinkedAccount.getCreatedAt().format(LINKED_AT_FORMATTER)
		);
	}

	private void validateAccountNumber(String accountNumber) {
		if (accountNumber == null || !accountNumber.matches("^\\d{11}$")) {
			throw new IllegalArgumentException(INVALID_ACCOUNT_MESSAGE);
		}
	}
}
