package com.hanaro.hanaconnect.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CustomAPIResponse<T> {

	private int status;
	private T data;
	private String message;

	// 성공
	public static <T> CustomAPIResponse<T> createSuccess(int status, T data, String message) {
		return new CustomAPIResponse<T>(status, data, message);
	}

	// 실패
	public static <T> CustomAPIResponse<T> createFail(int status, String message) {
		return new CustomAPIResponse<T>(status, null, message);
	}

	// 실패(data 있음)
	public static <T> CustomAPIResponse<T> createFail(int status, T data, String message) {
		return new CustomAPIResponse<>(status, data, message);
	}
}
