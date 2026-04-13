package com.hanaro.hanaconnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.dto.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.dto.WalletResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
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

	@Override
	public WalletResponseDTO getMyWallet(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

		return WalletResponseDTO.builder()
			.walletMoney(member.getWalletMoney())
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
}
