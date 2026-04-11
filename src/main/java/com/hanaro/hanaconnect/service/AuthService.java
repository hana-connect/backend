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

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public LoginResponseDTO login(LoginRequestDTO request) {
		Member member = memberRepository.findById(request.getMemberId())
			.orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("회원을 찾을 수 없습니다."));

		if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
			throw new BadCredentialsException("간편비밀번호가 일치하지 않습니다.");
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
