package com.hanaro.hanaconnect.service;

import com.hanaro.hanaconnect.common.enums.HouseLevel;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.util.HouseLevelCalculator;
import com.hanaro.hanaconnect.dto.HouseStatusResponseDTO;
import com.hanaro.hanaconnect.entity.House;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.repository.HouseRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HouseService {

	private final HouseRepository houseRepository;
	private final MemberRepository memberRepository;
	private final RelationRepository relationRepository;
	private final PhoneNameRepository phoneNameRepository;

	public HouseStatusResponseDTO getHouseStatus(Long requesterId, Long kidId) {
		Member requester = memberRepository.findById(requesterId)
			.orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다."));

		Member kid = resolveTargetKid(requester, kidId);

		Optional<House> houseOpt = houseRepository.findByMemberId(kid.getId());

		if (houseOpt.isEmpty()) {
			return HouseStatusResponseDTO.builder()
				.memberId(kid.getId())
				.level(0)
				.gauge(0)
				.totalCount(null)
				.monthlyPayment(null)
				.startDate(null)
				.message(null)
				.build();
		}

		House house = houseOpt.get();
		int level = HouseLevelCalculator.calculateLevel(house.getStartDate(), house.getTotalCount());
		int gauge = HouseLevelCalculator.calculateGauge(house.getTotalCount());
		HouseLevel houseLevel = HouseLevel.from(level);

		String message = buildMessage(requester, kid, houseLevel, house.getTotalCount());

		return HouseStatusResponseDTO.builder()
			.memberId(kid.getId())
			.level(level)
			.gauge(gauge)
			.totalCount(house.getTotalCount())
			.monthlyPayment(house.getMonthlyPayment())
			.startDate(house.getStartDate())
			.message(message)
			.build();
	}

	private Member resolveTargetKid(Member requester, Long kidId) {
		// 아이 본인 요청
		if (requester.getMemberRole() == MemberRole.KID) {
			return requester;
		}

		// 조부모: kidId 필수
		if (kidId == null) {
			throw new IllegalArgumentException("kidId는 필수입니다.");
		}

		Member kid = memberRepository.findById(kidId)
			.orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다."));

		// 기존 RelationRepository 메서드 재사용
		boolean hasRelation = relationRepository
			.existsByMember_IdAndConnectMember_Id(requester.getId(), kidId);
		if (!hasRelation) {
			throw new AccessDeniedException("해당 아이의 정보에 접근할 수 없습니다.");
		}

		return kid;
	}

	/**
	 * 메시지 생성
	 * - 조부모가 요청한 경우: "{조부모 이름}가 놓아주신..." 형태
	 * - 아이 본인: 기본 메시지
	 */
	private String buildMessage(Member requester, Member kid, HouseLevel houseLevel, int totalCount) {
		if (requester.getMemberRole() == MemberRole.PARENT) {
			Optional<PhoneName> phoneName = phoneNameRepository
				.findByWhoIdAndWhomId(kid.getId(), requester.getId());

			String connectorName = phoneName.map(PhoneName::getWhomName).orElse(requester.getName());
			return houseLevel.getPersonalizedMessage(connectorName, totalCount);
		}

		return houseLevel.getDefaultMessage(totalCount);
	}
}
