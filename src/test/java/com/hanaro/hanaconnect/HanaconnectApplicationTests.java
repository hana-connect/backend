package com.hanaro.hanaconnect;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
// @ActiveProfiles("local")
@Disabled("환경별 설정 분리 이후 기본 contextLoads 테스트는 중복되어 비활성화")
class HanaconnectApplicationTests {

	@Test
	void contextLoads() {
	}

}
