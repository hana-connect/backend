package com.hanaro.hanaconnect.service;

import com.hanaro.hanaconnect.dto.WalletResponseDTO;

public interface MemberService {
	WalletResponseDTO getMyWallet(Long memberId);
}
