package com.hanaro.hanaconnect.service;

import java.util.List;

import com.hanaro.hanaconnect.dto.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.dto.AccountVerifyRequestDTO;
import com.hanaro.hanaconnect.dto.AccountVerifyResponseDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddRequestDTO;
import com.hanaro.hanaconnect.dto.KidAccountAddResponseDTO;
import com.hanaro.hanaconnect.dto.KidAccountListResponseDTO;
import com.hanaro.hanaconnect.dto.KidWalletDetailResponseDTO;
import com.hanaro.hanaconnect.dto.MyAccountResponseDTO;
import com.hanaro.hanaconnect.dto.TerminatedAccountResponseDTO;

public interface AccountService {

	AccountLinkResponseDTO linkMyAccount(Long memberId, AccountLinkRequestDTO request);

	AccountVerifyResponseDTO verifyMyAccount(Long memberId, AccountVerifyRequestDTO request);

	KidAccountAddResponseDTO addKidAccount(Long memberId, Long kidId, KidAccountAddRequestDTO request);

	List<MyAccountResponseDTO> getMyAccounts(Long memberId, Integer limit);

	List<KidAccountListResponseDTO> getKidAccounts(Long memberId, Integer limit);

	List<TerminatedAccountResponseDTO> getTerminatedSavings(Long memberId);

	KidWalletDetailResponseDTO getKidLinkedAccounts(Long parentId, Long kidId);
}
