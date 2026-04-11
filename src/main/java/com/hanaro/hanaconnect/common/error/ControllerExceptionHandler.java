package com.hanaro.hanaconnect.common.error;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ControllerExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
		return ResponseEntity.badRequest()
			.body(CustomAPIResponse.createFail(
				HttpStatus.BAD_REQUEST.value(),
				Objects.toString(e.getMessage(), "잘못된 요청입니다.")
			));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException e) {

		String message = e.getBindingResult().getAllErrors().stream()
			.findFirst()
			.map(fe -> Objects.toString(fe.getDefaultMessage(), "잘못된 요청입니다."))
			.orElse("잘못된 요청입니다.");

		return ResponseEntity.badRequest()
			.body(CustomAPIResponse.createFail(
				HttpStatus.BAD_REQUEST.value(),
				message
			));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleConstraintViolationException(
		ConstraintViolationException e) {

		String message = e.getConstraintViolations().stream()
			.findFirst()
			.map(v -> Objects.toString(v.getMessage(), "잘못된 요청입니다."))
			.orElse("잘못된 요청입니다.");

		return ResponseEntity.badRequest()
			.body(CustomAPIResponse.createFail(
				HttpStatus.BAD_REQUEST.value(),
				message
			));
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleAuthorizationDeniedException(
		AuthorizationDeniedException e) {

		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(CustomAPIResponse.createFail(
				HttpStatus.FORBIDDEN.value(),
				"접근 권한이 없습니다."
			));
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleNoHandlerFoundException(
		NoHandlerFoundException e) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(CustomAPIResponse.createFail(
				HttpStatus.NOT_FOUND.value(),
				"요청 경로를 찾을 수 없습니다."
			));
	}

	@ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleBadCredentialsException(
		org.springframework.security.authentication.BadCredentialsException e) {

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(CustomAPIResponse.createFail(
				HttpStatus.UNAUTHORIZED.value(),
				Objects.toString(e.getMessage(), "인증에 실패했습니다.")
			));
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleEntityNotFoundException(
		EntityNotFoundException e) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(CustomAPIResponse.createFail(
				HttpStatus.NOT_FOUND.value(),
				Objects.toString(e.getMessage(), "대상을 찾을 수 없습니다.")
			));
	}

	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleSpringAccessDeniedException(
		org.springframework.security.access.AccessDeniedException e) {

		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(CustomAPIResponse.createFail(
				HttpStatus.FORBIDDEN.value(),
				"접근 권한이 없습니다."
			));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CustomAPIResponse<?>> handleException(Exception e) {
		e.printStackTrace();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(CustomAPIResponse.createFail(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"서버 에러가 발생했습니다."
			));
	}
}
