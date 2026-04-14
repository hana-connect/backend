package com.hanaro.hanaconnect.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Component
public class AssetAiClient {

	private final RestClient restClient;

	@Value("${asset-ai.api-key}")
	private String apiKey;

	@Value("${asset-ai.model}")
	private String model;

	@Value("${asset-ai.base-url}")
	private String baseUrl;

	public AssetAiClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();
	}

	public String getAssetRecommendationFromAi(String prompt) {
		Map<String, Object> requestBody = Map.of(
			"model", model,
			"messages", List.of(
				Map.of("role", "developer", "content", "Return only valid JSON matching the schema."),
				Map.of("role", "user", "content", prompt)
			),
			"response_format", Map.of(
				"type", "json_schema",
				"json_schema", Map.of(
					"name", "asset_recommendation",
					"schema", Map.of(
						"type", "object",
						"properties", Map.of(
							"recommendRatio", Map.of("type", "string"),
							"kidAllowance", Map.of("type", "integer"),
							"aiComment", Map.of("type", "string"),
							"increaseRate", Map.of("type", "integer")
						),
						"required", List.of("recommendRatio", "kidAllowance", "aiComment", "increaseRate"),
						"additionalProperties", false
					)
				)
			)
		);

		AssetAiChatResponseDTO response = restClient.post()
			.uri(baseUrl + "/chat/completions")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(requestBody)
			.retrieve()
			.body(AssetAiChatResponseDTO.class);

		// 검증 로직
		if (response == null
			|| response.choices() == null
			|| response.choices().isEmpty()
			|| response.choices().get(0).message() == null
			|| response.choices().get(0).message().content() == null) {
			throw new RuntimeException("Asset AI 응답이 비어 있습니다.");
		}

		return response.choices().get(0).message().content();
	}
}
