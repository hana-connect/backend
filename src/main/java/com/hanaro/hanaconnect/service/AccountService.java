package com.hanaro.hanaconnect.service;

import com.hanaro.hanaconnect.dto.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddRequestDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddResponseDTO;

public interface AccountService {

	AccountLinkResponseDTO linkMyAccount(Long memberId, AccountLinkRequestDTO request);

	KidAccountAddResponseDTO addKidAccount(Long memberId, Long kidId, KidAccountAddRequestDTO request);
}
