package com.hanaro.hanaconnect.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtTokenProvider {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.access-token-expiration-ms}")
	private long accessTokenExpirationMs;

	private SecretKey key;

	@PostConstruct
	public void init() {
		this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(TokenMemberPrincipal principal) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

		return Jwts.builder()
			.subject(String.valueOf(principal.getMemberId()))
			.claim("name", principal.getName())
			.claim("virtualAccount", principal.getVirtualAccount())
			.claim("memberRole", principal.getMemberRole().name())
			.claim("role", principal.getRole().name())
			.issuedAt(now)
			.expiration(expiry)
			.signWith(key)
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public TokenMemberPrincipal getPrincipal(String token) {
		Claims claims = Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();

		Long memberId = Long.parseLong(claims.getSubject());
		String name = claims.get("name", String.class);
		String virtualAccount = claims.get("virtualAccount", String.class);
		MemberRole memberRole = MemberRole.valueOf(claims.get("memberRole", String.class));
		Role role = Role.valueOf(claims.get("role", String.class));

		return new TokenMemberPrincipal(memberId, name, virtualAccount, memberRole, role);
	}
}
