package com.hanaro.hanaconnect.common.enums;

import com.hanaro.hanaconnect.common.util.KoreanFormatter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HouseLevel {
	LEVEL_0(0, null, null, "텅"),

	LEVEL_1(1, "나무 한 그루가 자라났어요! {totalCount}개월의 작은 시작이 우리 별돌이의 꿈을 키워가고 있어요.", "심어주신", "나무 심기"),
	LEVEL_2(2, "벽돌 덕분에 기초가 쌓이기 시작했어요! {totalCount}개월 동안 차곡차곡 쌓인 마음이 집의 첫 모습을 만들어가고 있어요.", "놓아주신", "벽돌 4개 추가"),
	LEVEL_3(3, "벽돌이 점점 높아지고 있어요! {totalCount}개월 동안 한결같이 쌓인 마음이 우리 별돌이의 집을 든든하게 세우고 있어요.", "쌓아주신", "벽돌 6개 추가"),
	LEVEL_4(4, "벽돌 덕분에 집이 점점 높아지고 있어요! {totalCount}개월의 꾸준한 납입이 우리 별돌이의 꿈을 한 층씩 올려가고 있어요.", "쌓아주신", "벽돌 8개 추가"),
	LEVEL_5(5, "이번 달 벽돌 덕분에 드디어 벽과 문이 생겼어요! {totalCount}개월 동안 한결같이 쌓인 이 단단한 마음이 우리 별돌이의 꿈을 지켜줄 거예요.", "놓아주신", "벽 1개, 문 추가"),
	LEVEL_6(6, "이번 달 벽돌 덕분에 지붕이 한 뼘 더 높아졌어요! {totalCount}개월 동안 한결같이 쌓인 이 단단한 마음이 우리 별돌이의 꿈을 지켜줄 거예요.", "놓아주신", "벽 1개 추가"),
	LEVEL_7(7, "창문이 생겼어요! {totalCount}개월 동안 쌓인 따뜻한 마음이 창문 너머로 환하게 빛나고 있어요.", "달아주신", "창문 추가"),
	LEVEL_8(8, "지붕이 완성됐어요! {totalCount}개월 동안 비바람을 막아줄 든든한 지붕이 생겼어요.", "완성해주신", "지붕 추가"),
	LEVEL_9(9, "굴뚝이 생겼어요! {totalCount}개월 동안 쌓인 따뜻한 마음이 연기처럼 피어오르고 있어요.", "만들어주신", "굴뚝 추가"),
	LEVEL_10(10, "울타리가 생겼어요! {totalCount}개월 동안 쌓인 사랑이 우리 별돌이의 집을 포근하게 감싸주고 있어요.", "세워주신", "울타리 추가"),
	LEVEL_11(11, "예쁜 연못이 생겼어요! {totalCount}개월 동안 한결같이 이어온 마음이 정원을 아름답게 가꾸고 있어요.", "만들어주신", "연못 추가"),
	LEVEL_12(12, "가로등이 밝혀졌어요! {totalCount}개월 동안 쌓인 빛나는 마음이 밤에도 집을 환하게 밝혀줄 거예요.", "밝혀주신", "가로등 추가"),
	LEVEL_13(13, "아늑한 벤치가 생겼어요! {totalCount}개월 동안 함께 걸어온 이 길이 우리 별돌이에게 편안한 쉼터가 되어줄 거예요.", "놓아주신", "벤치 추가"),
	LEVEL_14(14, "2층 벽돌이 쌓이기 시작했어요! {totalCount}개월 동안 쌓아온 꿈이 드디어 하늘 위로 올라가고 있어요.", "쌓아주신", "2층집 벽돌 추가"),
	LEVEL_15(15, "드디어 2층집이 완성됐어요! {totalCount}개월 동안 한결같이 쌓아온 사랑이 우리 별돌이의 꿈을 이루어냈어요.", "함께해주신", "2층집 완성");

	private final int level;
	private final String messageTemplate;
	private final String action;
	private final String changeDescription;

	public static HouseLevel from(int level) {
		for (HouseLevel hl : values()) {
			if (hl.level == level) return hl;
		}
		return LEVEL_0;
	}

	public String getDefaultMessage(int totalCount) {
		if (messageTemplate == null) return null;

		return messageTemplate.replace("{totalCount}", String.valueOf(totalCount));
	}

	public String getPersonalizedMessage(String name, int totalCount) {
		if (messageTemplate == null) return null;

		String subject = KoreanFormatter.getSubjectParticle(name);
		String prefix = name + subject + " " + action + " 덕분에 ";

		return prefix + messageTemplate.replace("{totalCount}", String.valueOf(totalCount));
	}
}
