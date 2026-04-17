package com.hanaro.hanaconnect.service;

import java.util.List;

import com.hanaro.hanaconnect.dto.account.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.dto.account.WalletResponseDTO;

public interface MemberService {
	WalletResponseDTO getMyWallet(Long memberId);
	List<ConnectMemberResponseDTO> getParents(Long memberId);
	List<ConnectMemberResponseDTO> getKids(Long memberId);
	List<ConnectMemberResponseDTO> getOtherParents(Long memberId, Long kidId);
}
