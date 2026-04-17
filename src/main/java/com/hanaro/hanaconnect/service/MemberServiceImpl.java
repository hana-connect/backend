package com.hanaro.hanaconnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.AccountType;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.dto.account.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.dto.account.WalletResponseDTO;
import com.hanaro.hanaconnect.entity.Account;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.AccountRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService{

	private final MemberRepository memberRepository;
	private final RelationRepository relationRepository;
	private final AccountRepository accountRepository;

	@Override
	public WalletResponseDTO getMyWallet(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

		Account walletAccount = accountRepository
			.findByMemberIdAndAccountType(memberId, AccountType.WALLET)
			.orElseThrow(() -> new IllegalArgumentException("지갑 계좌가 존재하지 않습니다."));

		return WalletResponseDTO.builder()
			.name(member.getName())
			.walletMoney(walletAccount.getBalance())
			.build();
	}

	@Override
	public List<ConnectMemberResponseDTO> getParents(Long memberId) {
		return relationRepository.findParents(memberId);
	}

	@Override
	public List<ConnectMemberResponseDTO> getKids(Long memberId) {
		return relationRepository.findKids(memberId);
	}

	@Override
	public List<ConnectMemberResponseDTO> getOtherParents(Long memberId, Long kidId) {
		boolean isRelated = relationRepository
			.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
				kidId,
				memberId,
				MemberRole.PARENT
			);

		if (!isRelated) {
			return List.of();
		}

		return relationRepository.findOtherParents(memberId, kidId);
	}
}
