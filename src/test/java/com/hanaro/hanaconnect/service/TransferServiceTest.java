package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.dto.TransferPrepareResponseDto;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;

@ActiveProfiles("test")
@SpringBootTest
class TransferServiceTest {

	@Autowired
	private TransferService transferService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Test
	@DisplayName("송금 준비 조회 성공")
	void getTransferPrepareInfo_success() {
		// given
		Member parent = memberRepository.findAll().stream()
			.filter(member -> "김엄마".equals(member.getName()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("부모 더미데이터가 없습니다."));

		Account kidAccount = accountRepository.findAll().stream()
			.filter(account ->
				"아이 입출금 통장".equals(account.getName())
					&& account.getAccountType() == AccountType.FREE
			)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("아이 계좌 더미데이터가 없습니다."));

		// when
		TransferPrepareResponseDto result =
			transferService.getTransferPrepareInfo(parent.getId(), kidAccount.getId());

		// then
		assertThat(result).isNotNull();
		assertThat(result.getAccountId()).isEqualTo(kidAccount.getId());
		assertThat(result.getTargetMemberName()).isEqualTo("홍길동");
		assertThat(result.getPhoneSavedName()).isEqualTo("우리 아들");
		assertThat(result.getDisplayName()).isEqualTo("홍길동(우리 아들)");
		assertThat(result.getAccountAlias()).isEqualTo("아이 입출금 통장");
		assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("800000"));
	}

	@Test
	@DisplayName("송금 준비 조회 실패 - 존재하지 않는 계좌")
	void getTransferPrepareInfo_fail_invalidAccountId() {
		// given
		Member parent = memberRepository.findAll().stream()
			.filter(member -> "김엄마".equals(member.getName()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("부모 더미데이터가 없습니다."));

		Long invalidAccountId = 999999L;

		// when & then
		assertThatThrownBy(() ->
			transferService.getTransferPrepareInfo(parent.getId(), invalidAccountId)
		)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("존재하지 않는 계좌입니다.");
	}
}
