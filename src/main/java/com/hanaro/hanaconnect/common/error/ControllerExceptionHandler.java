package com.hanaro.hanaconnect.common.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.hanaro.hanaconnect.common.response.CustomAPIResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ControllerExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleIllegalExceptionHandler(IllegalArgumentException e) {

		return ResponseEntity.badRequest()
			.body(CustomAPIResponse.createFail(
				HttpStatus.BAD_REQUEST.value(),
				e.getMessage()
			));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleNotValidExceptionHandler(MethodArgumentNotValidException e) {

		Map<String, String> map = e.getBindingResult().getFieldErrors().stream()
			.collect(Collectors.toMap(
				FieldError::getField,
				fe -> Objects.toString(fe.getDefaultMessage(), "Not Valid!"),
				(existing, newValue) -> existing + ", " + newValue,
				LinkedHashMap::new
			));

		return ResponseEntity.badRequest()
			.body(CustomAPIResponse.createFail(
				HttpStatus.BAD_REQUEST.value(),
				map,
				"Validation Error"
			));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleViolationExceptionHandler(ConstraintViolationException e) {

		Map<String, String> map = e.getConstraintViolations().stream()
			.collect(Collectors.toMap(
				v -> v.getPropertyPath().toString(),
				v -> Objects.toString(v.getMessage(), "Violation Value!"),
				(existing, newValue) -> existing + ", " + newValue
			));

		return ResponseEntity.badRequest()
			.body(CustomAPIResponse.createFail(
				HttpStatus.BAD_REQUEST.value(),
				map,
				"Validation Error"
			));
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleAccessDeniedException(AuthorizationDeniedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(CustomAPIResponse.createFail(
				HttpStatus.FORBIDDEN.value(),
				e.getMessage()
			));
	}


	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<CustomAPIResponse<?>> handleNotFoundException(NoHandlerFoundException e) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(CustomAPIResponse.createFail(
				HttpStatus.NOT_FOUND.value(),
				"요청 경로를 찾을 수 없습니다."
			));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CustomAPIResponse<?>> handleOthersExceptionHandler(Exception e) {

		e.printStackTrace();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(CustomAPIResponse.createFail(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"서버 에러가 발생했습니다."
			));
	}
}
