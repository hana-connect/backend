package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.dto.ConnectMemberResponseDTO;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.entity.Relation;

class RelationRepositoryTest extends BaseRepositoryTest {

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PhoneNameRepository phoneNameRepository;

	private static long accountSeq = 10000000000L;

	private Member createMember(String name, MemberRole memberRole) {
		return memberRepository.save(
			Member.builder()
				.name(name)
				.password("123456")
				.birthday(LocalDate.of(2000, 1, 1))
				.virtualAccount(generateAccount())
				.walletMoney(BigDecimal.ZERO)
				.memberRole(memberRole)
				.role(Role.USER)
				.build()
		);
	}

	private Relation createRelation(Member member, Member connectMember) {
		return relationRepository.save(
			Relation.builder()
				.member(member)
				.connectMember(connectMember)
				.connectMemberRole(connectMember.getMemberRole())
				.build()
		);
	}

	private PhoneName createPhoneName(Member who, Member whom, String whomName) {
		return phoneNameRepository.save(
			PhoneName.builder()
				.who(who)
				.whom(whom)
				.whomName(whomName)
				.build()
		);
	}

	@Test
	@DisplayName("부모 목록 조회")
	void findParentsTest() {
		Member kid = createMember("홍길동", MemberRole.KID);
		Member parent1 = createMember("김엄마", MemberRole.PARENT);
		Member parent2 = createMember("이할머니", MemberRole.PARENT);

		createRelation(kid, parent1);
		createRelation(kid, parent2);

		createPhoneName(kid, parent1, "우리 엄마");
		createPhoneName(kid, parent2, "외할머니");

		List<ConnectMemberResponseDTO> result = relationRepository.findParents(kid.getId());

		assertThat(result).hasSize(2);
		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberName)
			.containsExactlyInAnyOrder("김엄마", "이할머니");

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberRole)
			.containsOnly(MemberRole.PARENT);

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberPhoneName)
			.containsExactlyInAnyOrder("우리 엄마", "외할머니");
	}

	@Test
	@DisplayName("아이 목록 조회")
	void findKidsTest() {
		Member kid = createMember("홍길동", MemberRole.KID);
		Member parent = createMember("김엄마", MemberRole.PARENT);
		Member grandParent = createMember("이할머니", MemberRole.PARENT);

		createRelation(parent, kid);
		createRelation(parent, grandParent); // 부모-부모 연결도 추가

		createPhoneName(parent, kid, "우리 아들");
		createPhoneName(parent, grandParent, "친정 엄마");

		List<ConnectMemberResponseDTO> result = relationRepository.findKids(parent.getId());

		assertThat(result).hasSize(1);
		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberName)
			.containsExactly("홍길동");

		assertThat(result)
			.extracting(ConnectMemberResponseDTO::getConnectMemberRole)
			.containsOnly(MemberRole.KID);

		assertThat(result.get(0).getConnectMemberPhoneName()).isEqualTo("우리 아들");
	}

	@Test
	@DisplayName("연결 관계 존재 여부 확인")
	void existsByMemberIdAndConnectMemberIdAndConnectMemberRoleTest() {
		Member kid = createMember("홍길동", MemberRole.KID);
		Member parent = createMember("김엄마", MemberRole.PARENT);

		createRelation(kid, parent);

		boolean exists = relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
			kid.getId(),
			parent.getId(),
			MemberRole.PARENT
		);

		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("연결 관계 없으면 false 반환")
	void existsByMemberIdAndConnectMemberIdAndConnectMemberRoleFailTest() {
		Member kid = createMember("홍길동", MemberRole.KID);
		Member parent = createMember("김엄마", MemberRole.PARENT);

		boolean exists = relationRepository.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
			kid.getId(),
			parent.getId(),
			MemberRole.PARENT
		);

		assertThat(exists).isFalse();
	}

	// 무작위 생성
	private String generateAccount() {
		return String.valueOf(accountSeq++);
	}
}
