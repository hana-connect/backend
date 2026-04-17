package com.hanaro.hanaconnect.common.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();

		return path.startsWith("/api/auth/")
			|| path.equals("/swagger.html")
			|| path.startsWith("/swagger-ui/")
			|| path.startsWith("/hanaconnect/api-docs")
			|| path.startsWith("/v3/api-docs")
			|| path.equals("/favicon.ico");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);

			if (jwtTokenProvider.validateToken(token)) {
				TokenMemberPrincipal principal = jwtTokenProvider.getPrincipal(token);

				UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(
						principal,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole().name()))
					);

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}

		filterChain.doFilter(request, response);
	}
}
