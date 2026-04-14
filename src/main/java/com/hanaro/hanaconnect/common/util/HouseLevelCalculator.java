package com.hanaro.hanaconnect.common.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class HouseLevelCalculator {

	private static final int MAX_LEVEL = 15;
	private static final int MONTHS_PER_LEVEL = 12;

	/**
	 * 납입 시작일 기준으로 레벨 계산
	 * 시작하면 lv1, 이후 12개월마다 +1
	 */
	public static int calculateLevel(LocalDate startDate, int totalCount) {
		if (startDate == null || totalCount <= 0) return 0;

		long monthsElapsed = ChronoUnit.MONTHS.between(startDate, LocalDate.now());
		if (monthsElapsed < 0) return 0;
		int level = 1 + (int)(monthsElapsed / MONTHS_PER_LEVEL);
		return Math.min(level, MAX_LEVEL);
	}

	/**
	 * 게이지: 현재 레벨 내에서의 진행도 (0~100)
	 * 1년(12회) = 100, 한 번 납입 시 100/12
	 */
	public static int calculateGauge(int totalCount) {
		if (totalCount <= 0) return 0;
		int countWithinLevel = totalCount % MONTHS_PER_LEVEL;
		if (countWithinLevel == 0) return 100;
		return (int) Math.round((countWithinLevel / (double) MONTHS_PER_LEVEL) * 100);
	}
}
