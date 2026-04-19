package com.hanaro.hanaconnect.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.Role;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.PhoneName;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DataJpaTest
class PhoneNameRepositoryTest {

	@Autowired
	private PhoneNameRepository phoneNameRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	@DisplayName("전화번호 저장 이름 조회 성공")
	void findNameByOwnerIdAndTargetId_success() {
		// given
		Member who = memberRepository.save(Member.builder()
			.name("부모")
			.password("1234")
			.virtualAccount("11111111111")
			.birthday(LocalDate.now())
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build());

		Member whom = memberRepository.save(Member.builder()
			.name("자식")
			.password("1234")
			.virtualAccount("22222222222")
			.birthday(LocalDate.now())
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build());

		phoneNameRepository.save(
			PhoneName.builder()
				.who(who)
				.whom(whom)
				.whomName("우리 아들")
				.build()
		);

		// when
		Optional<String> result =
			phoneNameRepository.findNameByOwnerIdAndTargetId(who.getId(), whom.getId());

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo("우리 아들");
	}

	@Test
	@DisplayName("전화번호 저장 이름 없음 → empty 반환")
	void findNameByOwnerIdAndTargetId_notFound() {
		// given
		Member who = memberRepository.save(Member.builder()
			.name("부모")
			.password("1234")
			.virtualAccount("11111111111")
			.birthday(LocalDate.now())
			.memberRole(MemberRole.PARENT)
			.role(Role.USER)
			.build());

		Member whom = memberRepository.save(Member.builder()
			.name("자식")
			.password("1234")
			.virtualAccount("22222222222")
			.birthday(LocalDate.now())
			.memberRole(MemberRole.KID)
			.role(Role.USER)
			.build());

		// when
		Optional<String> result =
			phoneNameRepository.findNameByOwnerIdAndTargetId(who.getId(), whom.getId());

		// then
		assertThat(result).isEmpty();
	}
}
