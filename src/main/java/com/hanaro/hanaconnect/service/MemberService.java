package com.hanaro.hanaconnect.service;

import java.util.List;

import com.hanaro.hanaconnect.dto.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.dto.WalletResponseDTO;

public interface MemberService {
	WalletResponseDTO getMyWallet(Long memberId);
	List<ConnectMemberResponseDTO> getParents(Long memberId);
}
