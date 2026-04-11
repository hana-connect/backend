package com.hanaro.hanaconnect.service;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.dto.WalletResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService{

	private final MemberRepository memberRepository;

	@Override
	public ResponseEntity<CustomAPIResponse<?>> getMyWallet(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

		WalletResponseDTO walletResponseDTO = WalletResponseDTO.builder()
			.walletMoney(member.getWalletMoney())
			.build();

		return ResponseEntity.status(HttpStatus.OK)
			.body(CustomAPIResponse.createSuccess(200, walletResponseDTO, "내 지갑 잔액 조회를 성공했습니다."));
	}
}
