package com.hanaro.hanaconnect.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HouseLevel {
	LEVEL_0(0, "텅 빈 땅이에요. 청약을 시작해서 집을 지어볼까요?", "텅"),
	LEVEL_1(1, "나무 한 그루가 자라났어요! 청약의 첫걸음을 내딛었어요.", "나무 심기"),
	LEVEL_2(2, "벽돌 4개가 쌓였어요! 집의 기초가 만들어지고 있어요.", "벽돌 4개 추가"),
	LEVEL_3(3, "벽돌이 더 쌓였어요! 꾸준한 납입이 집을 만들어가요.", "벽돌 6개 추가"),
	LEVEL_4(4, "벽돌이 점점 높아지고 있어요!", "벽돌 8개 추가"),
	LEVEL_5(5, "벽과 문이 생겼어요! 집의 모양이 갖춰지기 시작했어요.", "벽 1개, 문 추가"),
	LEVEL_6(6, "벽이 한 뼘 더 높아졌어요!", "벽 1개 추가"),
	LEVEL_7(7, "창문이 생겼어요! 따뜻한 빛이 들어올 거예요.", "창문 추가"),
	LEVEL_8(8, "지붕이 완성됐어요! 비도 맞지 않겠네요.", "지붕 추가"),
	LEVEL_9(9, "굴뚝이 생겼어요! 따뜻한 집이 되어가고 있어요.", "굴뚝 추가"),
	LEVEL_10(10, "울타리가 생겼어요! 아늑한 공간이 만들어졌어요.", "울타리 추가"),
	LEVEL_11(11, "연못이 생겼어요! 정원이 아름다워지고 있어요.", "연못 추가"),
	LEVEL_12(12, "가로등이 밝혀졌어요! 밤에도 환한 집이에요.", "가로등 추가"),
	LEVEL_13(13, "벤치가 생겼어요! 쉬어갈 수 있는 공간이 생겼어요.", "벤치 추가"),
	LEVEL_14(14, "2층 벽돌이 쌓이기 시작했어요!", "2층집 벽돌 추가"),
	LEVEL_15(15, "2층집이 완성됐어요! 드디어 꿈의 집이 완성됐어요!", "2층집 완성");

	private final int level;
	private final String defaultMessage;
	private final String changeDescription;

	public static HouseLevel from(int level) {
		for (HouseLevel hl : values()) {
			if (hl.level == level) return hl;
		}
		return LEVEL_0;
	}

	public String getPersonalizedMessage(String connectorName, int totalCount) {
		if (connectorName != null && !connectorName.isBlank()) {
			return connectorName + "가 놓아주신 이번 달 벽돌 덕분에 지붕이 한 뼘 더 높아졌어요! "
				+ totalCount + "개월 동안 한결같이 쌓인 이 단단한 마음이 우리 별돌이의 꿈을 지켜줄 거예요.";
		}
		return defaultMessage;
	}
}
