package com.hanaro.hanaconnect.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.List;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.dto.asset.AssetSummaryResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.LinkedAccount;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.LinkedAccountRepository;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

	@Mock
	private LinkedAccountRepository linkedAccountRepository;

	@InjectMocks
	private AssetService assetService;

	private final Long TEST_MEMBER_ID = 100L;

	@Test
	void shouldReturnAssetSummary_whenAccountsExist() {
		// given
		Long memberId = TEST_MEMBER_ID;

		Member member = Member.builder()
			.id(memberId)
			.build();

		Account depositAccount = Account.builder()
			.accountType(AccountType.DEPOSIT)
			.balance(new BigDecimal("1000000"))
			.member(member)
			.build();

		Account pensionAccount = Account.builder()
			.accountType(AccountType.PENSION)
			.balance(new BigDecimal("500000"))
			.member(member)
			.build();

		LinkedAccount linked1 = LinkedAccount.builder()
			.account(depositAccount)
			.member(member)
			.build();

		LinkedAccount linked2 = LinkedAccount.builder()
			.account(pensionAccount)
			.member(member)
			.build();

		given(linkedAccountRepository.findAllByMemberId(anyLong()))
			.willReturn(List.of(linked1, linked2));

		// when
		AssetSummaryResponseDTO result = assetService.getMemberAssetSummary(memberId);

		// then
		assertAll(
			() -> assertEquals(new BigDecimal("1000000"), result.getDepositSavings()),
			() -> assertEquals(new BigDecimal("500000"), result.getPension()),
			() -> assertEquals(new BigDecimal("1500000"), result.getTotalAssets())
		);

		verify(linkedAccountRepository).findAllByMemberId(memberId);
	}

	@Test
	void shouldThrowException_whenNoLinkedAccounts() {
		// given
		Long memberId = TEST_MEMBER_ID;

		given(linkedAccountRepository.findAllByMemberId(anyLong()))
			.willReturn(List.of());

		// when & then
		EntityNotFoundException exception = assertThrows(
			EntityNotFoundException.class,
			() -> assetService.getMemberAssetSummary(memberId)
		);

		assertEquals("연결된 계좌가 없습니다.", exception.getMessage());
		verify(linkedAccountRepository).findAllByMemberId(memberId);
	}

	@Test
	void shouldSumDifferentAccountTypesCorrectly() {
		// given
		Long memberId = TEST_MEMBER_ID;
		Member member = Member.builder().id(memberId).build();

		Account savings = Account.builder()
			.accountType(AccountType.SAVINGS)
			.balance(new BigDecimal("200000"))
			.member(member)
			.build();

		Account subscription = Account.builder()
			.accountType(AccountType.SUBSCRIPTION)
			.balance(new BigDecimal("300000"))
			.member(member)
			.build();

		Account free = Account.builder()
			.accountType(AccountType.FREE)
			.balance(new BigDecimal("100000"))
			.member(member)
			.build();

		Account investment = Account.builder()
			.accountType(AccountType.INVESTMENT)
			.balance(new BigDecimal("400000"))
			.member(member)
			.build();

		LinkedAccount l1 = LinkedAccount.builder().account(savings).member(member).build();
		LinkedAccount l2 = LinkedAccount.builder().account(subscription).member(member).build();
		LinkedAccount l3 = LinkedAccount.builder().account(free).member(member).build();
		LinkedAccount l4 = LinkedAccount.builder().account(investment).member(member).build();

		given(linkedAccountRepository.findAllByMemberId(memberId))
			.willReturn(List.of(l1, l2, l3, l4));

		// when
		AssetSummaryResponseDTO result = assetService.getMemberAssetSummary(memberId);

		// then
		assertAll(
			() -> assertEquals(new BigDecimal("500000"), result.getDepositSavings()),
			() -> assertEquals(new BigDecimal("100000"), result.getDepositWithdrawal()),
			() -> assertEquals(new BigDecimal("400000"), result.getInvestment()),
			() -> assertEquals(new BigDecimal("1000000"), result.getTotalAssets())
		);

		verify(linkedAccountRepository).findAllByMemberId(memberId);
	}
}
