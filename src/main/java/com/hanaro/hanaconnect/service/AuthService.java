package com.hanaro.hanaconnect.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.security.JwtTokenProvider;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.dto.LoginRequestDTO;
import com.hanaro.hanaconnect.dto.LoginResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private static final String LOGIN_FAILED_MESSAGE = "아이디 또는 비밀번호가 올바르지 않습니다.";

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public LoginResponseDTO login(LoginRequestDTO request) {
		Member member = memberRepository.findById(request.getMemberId())
			.orElseThrow(() -> new BadCredentialsException(LOGIN_FAILED_MESSAGE));

		if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
			throw new BadCredentialsException(LOGIN_FAILED_MESSAGE);
		}

		TokenMemberPrincipal principal = new TokenMemberPrincipal(
			member.getId(),
			member.getName(),
			member.getVirtualAccount(),
			member.getMemberRole(),
			member.getRole()
		);

		String accessToken = jwtTokenProvider.createAccessToken(principal);

		return new LoginResponseDTO(
			accessToken,
			"Bearer",
			member.getId(),
			member.getName(),
			member.getRole().name(),
			member.getMemberRole().name()
		);
	}
}
