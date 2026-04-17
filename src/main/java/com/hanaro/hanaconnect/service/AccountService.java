package com.hanaro.hanaconnect.service;

import java.util.List;

import com.hanaro.hanaconnect.dto.account.AccountLinkRequestDTO;
import com.hanaro.hanaconnect.dto.account.AccountLinkResponseDTO;
import com.hanaro.hanaconnect.dto.account.AccountVerifyRequestDTO;
import com.hanaro.hanaconnect.dto.account.AccountVerifyResponseDTO;
import com.hanaro.hanaconnect.dto.account.KidAccountAddRequestDTO;
import com.hanaro.hanaconnect.dto.account.KidAccountAddResponseDTO;
import com.hanaro.hanaconnect.dto.account.KidAccountListResponseDTO;
import com.hanaro.hanaconnect.dto.account.KidWalletDetailResponseDTO;
import com.hanaro.hanaconnect.dto.account.MyAccountResponseDTO;
import com.hanaro.hanaconnect.dto.account.TerminatedAccountResponseDTO;
import com.hanaro.hanaconnect.dto.account.RewardAccountResponseDTO;

public interface AccountService {

	AccountLinkResponseDTO linkMyAccount(Long memberId, AccountLinkRequestDTO request);

	AccountVerifyResponseDTO verifyMyAccount(Long memberId, AccountVerifyRequestDTO request);

	KidAccountAddResponseDTO addKidAccount(Long memberId, Long kidId, KidAccountAddRequestDTO request);

	List<MyAccountResponseDTO> getMyAccounts(Long memberId, Integer limit);

	List<KidAccountListResponseDTO> getKidAccounts(Long memberId, Integer limit);

	List<TerminatedAccountResponseDTO> getTerminatedSavings(Long memberId);

	RewardAccountResponseDTO getRewardAccount(Long memberId);

	RewardAccountResponseDTO updateRewardAccount(Long memberId, Long linkedAccountId);

	KidWalletDetailResponseDTO getKidLinkedAccounts(Long parentId, Long kidId);

}
