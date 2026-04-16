package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.dto.RelayResponseDTO;
import com.hanaro.hanaconnect.dto.SavingsDetailResponseDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferRequestDTO;
import com.hanaro.hanaconnect.dto.SavingsTransferResponseDTO;
import com.hanaro.hanaconnect.dto.TransferRequestDto;
import com.hanaro.hanaconnect.dto.TransferResponseDto;
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
			.filter(m -> "김엄마".equals(m.getName()))
			.filter(m -> m.getMemberRole() == MemberRole.PARENT)
			.findFirst()
			.orElseThrow();
	}

	private Account findParentFreeAccount(Long parentId) {
		return accountRepository.findAll().stream()
			.filter(a -> a.getMember().getId().equals(parentId))
			.filter(a -> a.getAccountType() == AccountType.FREE)
			.findFirst()
			.orElseThrow();
	}

	private Account findLinkedKidSavingsAccount(Long parentId) {
		return accountRepository.findAll().stream()
			.filter(a -> a.getAccountType() == AccountType.SAVINGS)
			.filter(a -> linkedAccountRepository.existsByAccountIdAndMemberId(a.getId(), parentId))
			.findFirst()
			.orElseThrow();
	}

	private Account findLinkedKidFreeAccount(Long parentId) {
		return accountRepository.findAll().stream()
			.filter(a -> a.getAccountType() == AccountType.FREE)
			.filter(a -> linkedAccountRepository.existsByAccountIdAndMemberId(a.getId(), parentId))
			.findFirst()
			.orElseThrow();
	}

	@Test
	void transferToChildSavingsSuccessTest() {
		Member parent = findParent();
		Account sender = findParentFreeAccount(parent.getId());
		Account receiver = findLinkedKidSavingsAccount(parent.getId());

		BigDecimal senderBefore = sender.getBalance();
		BigDecimal receiverBefore = receiver.getBalance();

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(receiver.getId());
		request.setAmount(new BigDecimal("10000"));
		request.setPassword("123456");

		SavingsTransferResponseDTO result =
			transferService.transferToChildSavings(parent.getId(), request);

		assertThat(result).isNotNull();

		Account updatedSender = accountRepository.findById(sender.getId()).orElseThrow();
		Account updatedReceiver = accountRepository.findById(receiver.getId()).orElseThrow();

		assertThat(updatedSender.getBalance())
			.isEqualByComparingTo(senderBefore.subtract(new BigDecimal("10000")));
		assertThat(updatedReceiver.getBalance())
			.isEqualByComparingTo(receiverBefore.add(new BigDecimal("10000")));
	}

	@Test
	void getRelayHistorySuccessTest() {
		Member parent = findParent();
		Account receiver = findLinkedKidSavingsAccount(parent.getId());

		SavingsTransferRequestDTO request = new SavingsTransferRequestDTO();
		request.setTargetAccountId(receiver.getId());
		request.setAmount(new BigDecimal("50000"));
		request.setPassword("123456");
		request.setContent("용돈");

		transferService.transferToChildSavings(parent.getId(), request);

		RelayResponseDTO result =
			transferService.getRelayHistory(parent.getId(), receiver.getId(), 0);

		assertThat(result).isNotNull();
		assertThat(result.getHistory()).isNotEmpty();
	}

	@Test
	void getExpiredSavingsDetail_Success() {
		Account acc = accountRepository.findAll().stream()
			.filter(a -> Boolean.TRUE.equals(a.getIsEnd()))
			.filter(a -> a.getAccountType() == AccountType.SAVINGS)
			.findFirst()
			.orElseThrow();

		Long ownerId = acc.getMember().getId();

		SavingsDetailResponseDTO result =
			transferService.getExpiredSavingsDetail(ownerId, acc.getId(), 0, null);

		assertThat(result).isNotNull();
		assertThat(result.getTransactions()).isNotNull();
	}

	@Test
	void transferSuccessTest() {
		Member parent = findParent();
		Account sender = findParentFreeAccount(parent.getId());
		Account receiver = findLinkedKidFreeAccount(parent.getId());

		BigDecimal senderBefore = sender.getBalance();
		BigDecimal receiverBefore = receiver.getBalance();

		TransferRequestDto request = TransferRequestDto.builder()
			.accountId(receiver.getId())
			.amount(new BigDecimal("10000"))
			.password("123456")
			.build();

		TransferResponseDto result =
			transferService.transfer(parent.getId(), request);

		assertThat(result).isNotNull();

		Account updatedSender = accountRepository.findById(sender.getId()).orElseThrow();
		Account updatedReceiver = accountRepository.findById(receiver.getId()).orElseThrow();

		assertThat(updatedSender.getBalance())
			.isEqualByComparingTo(senderBefore.subtract(new BigDecimal("10000")));
		assertThat(updatedReceiver.getBalance())
			.isEqualByComparingTo(receiverBefore.add(new BigDecimal("10000")));
	}
}
