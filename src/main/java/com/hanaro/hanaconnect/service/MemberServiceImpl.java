package com.hanaro.hanaconnect.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.MemberRole;
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
			.name(member.getName())
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

	@Override
	public List<ConnectMemberResponseDTO> getOtherParents(Long memberId, Long kidId) {
		boolean isRelated = relationRepository
			.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
				kidId,          // member (아이)
				memberId,       // connectMember (부모)
				MemberRole.PARENT
			);

		if (!isRelated) {
			throw new IllegalArgumentException("해당 아이와 연결된 부모만 조회할 수 있습니다.");
		}

		return relationRepository.findOtherParents(memberId, kidId);
	}
}
