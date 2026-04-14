package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class TransferServiceTest {

	@Autowired
	private TransferService transferService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private LinkedAccountRepository linkedAccountRepository;

	private Member findParent() {
		return memberRepository.findAll().stream()
			.filter(member -> "김엄마".equals(member.getName()))
			.filter(member -> member.getMemberRole() == MemberRole.PARENT)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 부모 회원(김엄마)을 찾을 수 없습니다."));
	}

	private Account findParentFreeAccount(Long parentId) {
		return accountRepository.findAll().stream()
			.filter(account -> account.getMember().getId().equals(parentId))
			.filter(account -> account.getAccountType() == AccountType.FREE)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 부모 FREE 계좌를 찾을 수 없습니다."));
	}

	private Account findLinkedKidSavingsAccount(Long parentId) {
		return accountRepository.findAll().stream()
			.filter(account -> account.getAccountType() == AccountType.SAVINGS)
			.filter(account -> linkedAccountRepository.existsByAccountIdAndMemberId(account.getId(), parentId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 연결된 아이 적금 계좌를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("적금 송금 성공")
	void transferToChildSavingsSuccessTest() {
		Member parent = findParent();
		Account parentFreeAccount = findParentFreeAccount(parent.getId());
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		BigDecimal senderBefore = parentFreeAccount.getBalance();
		BigDecimal receiverBefore = kidSavingsAccount.getBalance();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setAccountPassword("5678");
		request.setContent("적금 응원 편지");

		SavingsTransferResponseDTO result =
			transferService.transferToChildSavings(parent.getId(), request);

		assertThat(result).isNotNull();
		assertThat(result.getTransactionMoney()).isEqualByComparingTo("10000");
		assertThat(result.getMessage()).isEqualTo("적금 응원 편지");

		Account updatedSender = accountRepository.findById(parentFreeAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("부모 계좌를 다시 찾을 수 없습니다."));
		Account updatedReceiver = accountRepository.findById(kidSavingsAccount.getId())
			.orElseThrow(() -> new IllegalArgumentException("아이 적금 계좌를 다시 찾을 수 없습니다."));

		assertThat(updatedSender.getBalance())
			.isEqualByComparingTo(senderBefore.subtract(new BigDecimal("10000")));
		assertThat(updatedReceiver.getBalance())
			.isEqualByComparingTo(receiverBefore.add(new BigDecimal("10000")));
	}

	@Test
	@DisplayName("적금 송금 실패 - 계좌 비밀번호 불일치")
	void transferToChildSavingsFailWrongPasswordTest() {
		Member parent = findParent();
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setAccountPassword("0000");
		request.setContent("편지");

		assertThatThrownBy(() -> transferService.transferToChildSavings(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("계좌 비밀번호가 일치하지 않습니다.");
	}

	@Test
	@DisplayName("적금 릴레이 내역 조회 성공")
	void getRelayHistorySuccessTest() {
		// Given
		Member parent = findParent();
		Account kidSavingsAccount = findLinkedKidSavingsAccount(parent.getId());

		// 내역 만들기
		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("50000"));
		request.setPassword("123456");
		request.setContent("할머니가 주는 용돈이다!");

		transferService.transferToChildSavings(parent.getId(), request);

		// When
		com.hanaro.hanaconnect.dto.RelayResponseDTO result =
			transferService.getRelayHistory(parent.getId(), kidSavingsAccount.getId());

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getProductName()).isEqualTo(kidSavingsAccount.getName());

		// history 검증
		assertThat(result.getHistory()).isNotEmpty();
		assertThat(result.getHistory().get(0).getMessage()).isEqualTo("할머니가 주는 용돈이다!");
		assertThat(result.getHistory().get(0).getAmount()).isEqualByComparingTo("50000");
	}

	@Test
	@DisplayName("적금 릴레이 내역 조회 실패 - 존재하지 않는 계좌")
	void getRelayHistoryFailTest() {
		Member parent = findParent();
		Long invalidAccountId = 9999L;

		assertThatThrownBy(() -> transferService.getRelayHistory(parent.getId(), invalidAccountId))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("계좌를 찾을 수 없습니다.");
	}
}
