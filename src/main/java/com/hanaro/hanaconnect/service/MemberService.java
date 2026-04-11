package com.hanaro.hanaconnect.service;

import org.springframework.http.ResponseEntity;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;

public interface MemberService {
	ResponseEntity<CustomAPIResponse<?>> getMyWallet(Long memberId);
}
