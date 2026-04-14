package com.hanaro.hanaconnect.service;

import com.hanaro.hanaconnect.common.enums.HouseLevel;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.security.CustomUserDetails;
import com.hanaro.hanaconnect.common.util.HouseLevelCalculator;
import com.hanaro.hanaconnect.dto.HouseStatusResponseDTO;
import com.hanaro.hanaconnect.entity.House;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.exception.CustomException;
import com.hanaro.hanaconnect.exception.ErrorCode;
import com.hanaro.hanaconnect.repository.HouseRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import lombok.RequiredArgsConstructor;
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

	public HouseStatusResponseDTO getHouseStatus(CustomUserDetails userDetails, Long kidId) {
		Member requester = memberRepository.findById(userDetails.getMemberId())
			.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

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

	/**
	 * 요청자 역할에 따라 조회 대상 아이 결정
	 */
	private Member resolveTargetKid(Member requester, Long kidId) {
		if (requester.getMemberRole() == MemberRole.KID) {
			return requester;
		}

		if (kidId == null) {
			throw new CustomException(ErrorCode.KID_ID_REQUIRED);
		}

		Member kid = memberRepository.findById(kidId)
			.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

		boolean hasRelation = relationRepository
			.existsByMemberIdAndConnectMemberId(requester.getId(), kidId);
		if (!hasRelation) {
			throw new CustomException(ErrorCode.ACCESS_DENIED);
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
		return houseLevel.getDefaultMessage();
	}
}
