package com.hanaro.hanaconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;
import com.hanaro.hanaconnect.dto.login.LoginRequestDTO;
import com.hanaro.hanaconnect.dto.login.LoginResponseDTO;
import com.hanaro.hanaconnect.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<CustomAPIResponse<?>> login(
		@Valid @RequestBody LoginRequestDTO request
	) {
		LoginResponseDTO response = authService.login(request);

		return ResponseEntity.status(HttpStatus.OK)
			.body(CustomAPIResponse.createSuccess(200, response, "로그인 성공")
		);
	}
}
