package com.hanaro.hanaconnect.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaro.hanaconnect.ai.AssetAiClient;
import com.hanaro.hanaconnect.dto.AssetAIRecommendationResponseDTO;
import com.hanaro.hanaconnect.dto.AssetSummaryResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetAIServiceTest {

	@Mock
	private AssetService assetService;

	@Mock
	private AssetAiClient assetAiClient;

	private ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	private AssetAIService assetAIService;

	private final Long TEST_MEMBER_ID = 999L;

	@BeforeEach
	void setUp() {
		assetAIService = new AssetAIService(assetService, assetAiClient, objectMapper);
	}

	@Test
	void shouldReturnAIRecommendation_whenValidResponse() {
		Long memberId = TEST_MEMBER_ID;

		AssetSummaryResponseDTO summary = AssetSummaryResponseDTO.builder()
			.totalAssets(new BigDecimal("1000000"))
			.depositSavings(new BigDecimal("800000"))
			.pension(new BigDecimal("200000"))
			.build();

		given(assetService.getMemberAssetSummary(anyLong()))
			.willReturn(summary);

		String aiJson = """
            {
              "aiComment": "테스트 코멘트입니다.",
              "increaseRate": 10,
              "kidAllowance": 70000,
              "recommendRatio": "30:70"
            }
            """;

		given(assetAiClient.getAssetRecommendationFromAi(anyString()))
			.willReturn(aiJson);

		// when
		AssetAIRecommendationResponseDTO result =
			assetAIService.getAssetRecommendation(memberId);

		// then
		assertAll(
			() -> assertEquals("30:70", result.getRecommendRatio()),
			() -> assertEquals(new BigDecimal("70000"), result.getKidAllowance()),
			() -> assertEquals("테스트 코멘트입니다.", result.getAiComment()),
			() -> assertEquals(10, result.getIncreaseRate()),
			() -> assertEquals(4, result.getAssetHistory().size()),
			() -> assertEquals(new BigDecimal("1000000"), result.getAssetHistory().get(3))
		);

		verify(assetService).getMemberAssetSummary(memberId);
	}

	@Test
	void shouldClampIncreaseRate_between5And15() {
		// given
		Long memberId = TEST_MEMBER_ID;

		AssetSummaryResponseDTO summary = AssetSummaryResponseDTO.builder()
			.totalAssets(new BigDecimal("1000000"))
			.depositSavings(BigDecimal.ZERO)
			.pension(BigDecimal.ZERO)
			.build();

		given(assetService.getMemberAssetSummary(anyLong()))
			.willReturn(summary);

		String aiJson = """
            {
              "aiComment": "테스트",
              "increaseRate": 20,
              "kidAllowance": 50000,
              "recommendRatio": "20:80"
            }
            """;

		given(assetAiClient.getAssetRecommendationFromAi(anyString()))
			.willReturn(aiJson);

		// when
		AssetAIRecommendationResponseDTO result =
			assetAIService.getAssetRecommendation(memberId);

		// then
		assertEquals(15, result.getIncreaseRate());
		verify(assetService).getMemberAssetSummary(memberId);
	}

	@Test
	void shouldReturnFallback_whenAIResponseFails() {
		// given
		Long memberId = TEST_MEMBER_ID;

		AssetSummaryResponseDTO summary = AssetSummaryResponseDTO.builder()
			.totalAssets(new BigDecimal("500000"))
			.depositSavings(BigDecimal.ZERO)
			.pension(BigDecimal.ZERO)
			.build();

		given(assetService.getMemberAssetSummary(anyLong()))
			.willReturn(summary);

		// AI 클라이언트에서 예외 발생 시나리오
		given(assetAiClient.getAssetRecommendationFromAi(anyString()))
			.willThrow(new RuntimeException("AI 서비스 연결 실패"));

		// when
		AssetAIRecommendationResponseDTO result =
			assetAIService.getAssetRecommendation(memberId);

		// then
		assertAll(
			() -> assertEquals("10:90", result.getRecommendRatio()),
			() -> assertEquals(new BigDecimal("50000"), result.getKidAllowance()),
			() -> assertEquals("안정적인 관리를 추천드립니다.", result.getAiComment()),
			() -> assertEquals(0, result.getIncreaseRate()),
			() -> assertEquals(new BigDecimal("500000"), result.getAssetHistory().get(0))
		);

		verify(assetService).getMemberAssetSummary(memberId);
	}
}
