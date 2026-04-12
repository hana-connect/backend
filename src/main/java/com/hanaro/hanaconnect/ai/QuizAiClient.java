package com.hanaro.hanaconnect.ai;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class QuizAiClient {

	private final RestClient restClient;

	@Value("${quiz-ai.api-key}")
	private String apiKey;

	@Value("${quiz-ai.model}")
	private String model;

	@Value("${quiz-ai.base-url}")
	private String baseUrl;

	// 외부 API 호출하는 HTTP 클라이언트
	public QuizAiClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();
	}

	// prompt를 OpenAI에 보내고 -> 퀴즈 JSON 문자열 받아오기
	public String generateQuizFromAi(String prompt) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다.");
		}
		// 요청 Body 만들기 (OpenAI에 보내는 JSON 만들기)
		Map<String, Object> requestBody = Map.of(

			// 어떤 AI 쓸지
			"model", model,

			// messages (GPT에게 하는 말)
			"messages", List.of(
				Map.of(
					"role", "developer",   // 행동 규칙
					"content", "Return only valid JSON that matches the provided schema."
				),
				Map.of(
					"role", "user",   // 실제 질문
					"content", prompt
				)
			),

			// " 이 구조로 정확하게 만들어라 "
			"response_format", Map.of(
				"type", "json_schema",
				"json_schema", Map.of(
					"name", "quiz_generation_response",
					"schema", Map.of(
						"type", "object",
						"properties", Map.of(
							"questions", Map.of(
								"type", "array",
								"minItems", 3,
								"maxItems", 3,
								"items", Map.of(
									"type", "object",
									"properties", Map.of(
										"questionOrder", Map.of("type", "integer"),
										"question", Map.of("type", "string"),
										"choices", Map.of(
											"type", "array",
											"minItems", 4,
											"maxItems", 4,
											"items", Map.of("type", "string")
										),
										"correctIndex", Map.of("type", "integer"),
										"hint", Map.of("type", "string")
									),
									"required", List.of(
										"questionOrder",
										"question",
										"choices",
										"correctIndex",
										"hint"
									),
									"additionalProperties", false
								)
							)
						),
						"required", List.of("questions"),
						"additionalProperties", false
					)
				)
			)
		);

		System.out.println("OpenAI 호출 시작");
		// 실제 API 호출
		QuizAiChatResponseDTO response = restClient.post()
			.uri(baseUrl + "/chat/completions")  // 요청
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey) // 인증
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(requestBody)  // JSON 보내기 (우리가 만든 Map -> JSON으로 변환)
			.retrieve()
			.body(QuizAiChatResponseDTO.class);  // 응답 받기

		System.out.println("OpenAI 응답 수신 완료");
		System.out.println(response);
		// 응답 검증
		if (response == null
			|| response.choices() == null
			|| response.choices().isEmpty()
			|| response.choices().getFirst().message() == null
			|| response.choices().getFirst().message().content() == null) {
			throw new RuntimeException("OpenAI 응답이 비어 있습니다.");
		}

		// 최종본
		return response.choices().getFirst().message().content();
	}
}
