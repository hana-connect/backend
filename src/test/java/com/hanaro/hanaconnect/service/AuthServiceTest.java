package com.hanaro.hanaconnect.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.common.security.JwtTokenProvider;
import com.hanaro.hanaconnect.dto.login.LoginRequestDTO;
import com.hanaro.hanaconnect.dto.login.LoginResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;

class AuthServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@InjectMocks
	private AuthService authService;

	private Member member;
	private LoginRequestDTO request;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		member = Member.builder()
			.id(1L)
			.name("김꼬마")
			.password("encodedPassword")
			.birthday(LocalDate.of(2010, 1, 2))
			.virtualAccount("encryptedAccount")
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build();

		request = new LoginRequestDTO();
		request.setMemberId(1L);
		request.setPassword("123456");
	}

	@Test
	@DisplayName("로그인 성공")
	void login_success() {
		// given
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(passwordEncoder.matches("123456", "encodedPassword")).willReturn(true);
		given(jwtTokenProvider.createAccessToken(any())).willReturn("mock-access-token");

		// when
		LoginResponseDTO response = authService.login(request);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
		assertThat(response.getTokenType()).isEqualTo("Bearer");
		assertThat(response.getMemberId()).isEqualTo(1L);
		assertThat(response.getName()).isEqualTo("김꼬마");
		assertThat(response.getRole()).isEqualTo("USER");
		assertThat(response.getMemberRole()).isEqualTo("KID");

		verify(memberRepository).findById(1L);
		verify(passwordEncoder).matches("123456", "encodedPassword");
		verify(jwtTokenProvider).createAccessToken(any());
	}

	@Test
	@DisplayName("로그인 실패 - 존재하지 않는 회원")
	void login_fail_member_not_found() {
		// given
		given(memberRepository.findById(1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");

		verify(memberRepository).findById(1L);
		verify(passwordEncoder, never()).matches(anyString(), anyString());
		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	@Test
	@DisplayName("로그인 실패 - 비밀번호 불일치")
	void login_fail_wrong_password() {
		// given
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(passwordEncoder.matches("123456", "encodedPassword")).willReturn(false);

		// when & then
		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");

		verify(memberRepository).findById(1L);
		verify(passwordEncoder).matches("123456", "encodedPassword");
		verify(jwtTokenProvider, never()).createAccessToken(any());
	}
}
