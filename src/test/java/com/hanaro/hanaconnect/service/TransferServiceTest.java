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

	private Account findKidSavingsAccount() {
		return accountRepository.findAll().stream()
			.filter(account -> account.getMember().getName().equals("홍길동"))
			.filter(account -> account.getAccountType() == AccountType.SAVINGS)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 아이 적금 계좌를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("적금 송금 성공")
	void transferToChildSavingsSuccessTest() {
		Member parent = findParent();
		Account parentFreeAccount = findParentFreeAccount(parent.getId());
		Account kidSavingsAccount = findKidSavingsAccount();

		BigDecimal senderBefore = parentFreeAccount.getBalance();
		BigDecimal receiverBefore = kidSavingsAccount.getBalance();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setAccountPassword("1111"); // 테스트 더미 비밀번호에 맞게 수정 필요
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
	@DisplayName("적금 송금 실패 - 금액이 0원 이하")
	void transferToChildSavingsFailInvalidAmountTest() {
		Member parent = findParent();
		Account kidSavingsAccount = findKidSavingsAccount();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(BigDecimal.ZERO);
		request.setAccountPassword("5678");
		request.setContent("편지");

		assertThatThrownBy(() -> transferService.transferToChildSavings(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("송금 금액은 0보다 커야 합니다.");
	}

	@Test
	@DisplayName("적금 송금 실패 - 존재하지 않는 적금 계좌")
	void transferToChildSavingsFailInvalidTargetAccountTest() {
		Member parent = findParent();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(999999L);
		request.setAmount(new BigDecimal("10000"));
		request.setAccountPassword("1111");
		request.setContent("편지");

		assertThatThrownBy(() -> transferService.transferToChildSavings(parent.getId(), request))
			.isInstanceOf(Exception.class);
	}

	@Test
	@DisplayName("적금 송금 실패 - 계좌 비밀번호 불일치")
	void transferToChildSavingsFailWrongPasswordTest() {
		Member parent = findParent();
		Account kidSavingsAccount = findKidSavingsAccount();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(kidSavingsAccount.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setAccountPassword("0000");
		request.setContent("편지");

		assertThatThrownBy(() -> transferService.transferToChildSavings(parent.getId(), request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("계좌 비밀번호가 일치하지 않습니다.");
	}
}
