package com.hanaro.hanaconnect.service;

import java.time.format.DateTimeFormatter;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.util.AccountNumberFormatter;
import com.hanaro.hanaconnect.dto.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

	private static final DateTimeFormatter LINKED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	private final AccountRepository accountRepository;
	private final LinkedAccountRepository linkedAccountRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public AccountLinkResponseDTO linkMyAccount(Long memberId, AccountLinkRequestDTO request) {
		String normalizedAccountNumber = AccountNumberFormatter.normalize(request.getAccountNumber());

		validateAccountNumber(normalizedAccountNumber);

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다."));

		Account account = accountRepository.findByAccountNumber(normalizedAccountNumber)
			.orElseThrow(() -> new EntityNotFoundException("해당 계좌 번호를 찾을 수 없습니다."));

		if (!passwordEncoder.matches(request.getAccountPassword(), account.getPassword())) {
			throw new IllegalArgumentException("계좌 비밀번호가 일치하지 않습니다.");
		}

		if (!account.getMember().getId().equals(member.getId())) {
			throw new IllegalArgumentException("본인 명의의 계좌만 등록할 수 있습니다.");
		}

		if (linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), member.getId())) {
			throw new IllegalArgumentException("이미 등록된 계좌번호입니다.");
		}

		LinkedAccount linkedAccount = LinkedAccount.builder()
			.account(account)
			.member(member)
			.build();

		LinkedAccount savedLinkedAccount = linkedAccountRepository.save(linkedAccount);

		return new AccountLinkResponseDTO(
			AccountNumberFormatter.format(account.getAccountNumber()),
			savedLinkedAccount.getCreatedAt().format(LINKED_AT_FORMATTER)
		);
	}

	private void validateAccountNumber(String accountNumber) {
		if (accountNumber == null || !accountNumber.matches("^\\d{11}$")) {
			throw new IllegalArgumentException("계좌번호 형식이 올바르지 않습니다.");
		}
	}
}
