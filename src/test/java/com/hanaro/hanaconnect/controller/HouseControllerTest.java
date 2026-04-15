package com.hanaro.hanaconnect.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.security.JwtTokenProvider;
import com.hanaro.hanaconnect.common.security.TokenMemberPrincipal;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HouseControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	private Member kidWithHouse;
	private Member kidWithoutHouse;
	private Member relatedParent;
	private Member unrelatedParent;

	private String kidAccessToken;
	private String relatedParentAccessToken;
	private String unrelatedParentAccessToken;

	@BeforeEach
	void setUp() {
		kidWithHouse = findMember("김청약", MemberRole.KID);
		kidWithoutHouse = findMember("홍길동", MemberRole.KID);
		relatedParent = findMember("청약할머니", MemberRole.PARENT);
		unrelatedParent = findMember("김엄마", MemberRole.PARENT);

		kidAccessToken = createAccessToken(kidWithHouse);
		relatedParentAccessToken = createAccessToken(relatedParent);
		unrelatedParentAccessToken = createAccessToken(unrelatedParent);
	}

	@Test
	@DisplayName("아이가 본인 청약 리포트를 조회할 수 있다")
	void getHouseStatusByKidTest() throws Exception {
		mvc.perform(get("/api/house/status")
				.header("Authorization", "Bearer " + kidAccessToken))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 상태 조회 성공"))
			.andExpect(jsonPath("$.data.memberId").value(kidWithHouse.getId()))
			.andExpect(jsonPath("$.data.totalCount").value(28))
			.andExpect(jsonPath("$.data.monthlyPayment").value(200000))
			.andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.containsString("할머니")))
			.andDo(print());
	}

	@Test
	@DisplayName("조부모가 연결된 아이의 청약 리포트를 조회할 수 있다")
	void getHouseStatusByParentTest() throws Exception {
		mvc.perform(get("/api/house/status")
					.header("Authorization", "Bearer " + relatedParentAccessToken)
				.param("kidId", String.valueOf(kidWithHouse.getId())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 상태 조회 성공"))
			.andExpect(jsonPath("$.data.memberId").value(kidWithHouse.getId()))
			.andExpect(jsonPath("$.data.totalCount").value(28))
			.andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("할머니"))))
			.andDo(print());
	}

	@Test
	@DisplayName("청약이 없는 아이가 조회하면 level 0 상태를 반환한다")
	void getHouseStatusWithoutHouseTest() throws Exception {
		String token = createAccessToken(kidWithoutHouse);

		mvc.perform(get("/api/house/status")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("청약 상태 조회 성공"))
			.andExpect(jsonPath("$.data.memberId").value(kidWithoutHouse.getId()))
			.andExpect(jsonPath("$.data.level").value(0))
			.andExpect(jsonPath("$.data.gauge").value(0))
			.andExpect(jsonPath("$.data.totalCount").doesNotExist())
			.andDo(print());
	}

	@Test
	@DisplayName("조부모가 kidId 없이 요청하면 400을 반환한다")
	void getHouseStatusWithoutKidIdByParentTest() throws Exception {
		mvc.perform(get("/api/house/status")
				.header("Authorization", "Bearer " + relatedParentAccessToken))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}

	@Test
	@DisplayName("조부모가 관계 없는 아이를 조회하면 403을 반환한다")
	void getHouseStatusByUnrelatedParentTest() throws Exception {
		mvc.perform(get("/api/house/status")
				.header("Authorization", "Bearer " + unrelatedParentAccessToken)
				.param("kidId", String.valueOf(kidWithHouse.getId())))
			.andExpect(status().isForbidden())
			.andDo(print());
	}

	private Member findMember(String name, MemberRole memberRole) {
		return memberRepository.findAll().stream()
			.filter(member -> member.getName().equals(name))
			.filter(member -> member.getMemberRole() == memberRole)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("테스트용 회원을 찾을 수 없습니다. name=" + name));
	}

	private String createAccessToken(Member member) {
		TokenMemberPrincipal principal = new TokenMemberPrincipal(
			member.getId(),
			member.getName(),
			member.getVirtualAccount(),
			member.getMemberRole(),
			member.getRole()
		);
		return jwtTokenProvider.createAccessToken(principal);
	}
}
