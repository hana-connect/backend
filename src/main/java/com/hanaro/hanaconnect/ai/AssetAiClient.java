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
							"ratio", Map.of("type", "string"),
							"allowance", Map.of("type", "integer"),
							"comment", Map.of("type", "string")
						),
						"required", List.of("ratio", "allowance", "comment"),
						"additionalProperties", false
					)
				)
			)
		);

		// OpenAI 응답을 직접 받기 위해 Map으로 처리
		Map response = restClient.post()
			.uri(baseUrl + "/chat/completions")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(requestBody)
			.retrieve()
			.body(Map.class);

		List choices = (List) response.get("choices");
		Map firstChoice = (Map) choices.get(0);
		Map message = (Map) firstChoice.get("message");

		return (String) message.get("content");
	}
}
