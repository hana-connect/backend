package com.hanaro.hanaconnect.service;

import com.hanaro.hanaconnect.common.enums.HouseLevel;
import com.hanaro.hanaconnect.common.enums.MemberRole;
import com.hanaro.hanaconnect.common.enums.TransactionType;
import com.hanaro.hanaconnect.common.util.HouseLevelCalculator;
import com.hanaro.hanaconnect.dto.HouseHistoryItemDTO;
import com.hanaro.hanaconnect.dto.HouseHistoryResponseDTO;
import com.hanaro.hanaconnect.dto.HouseStatusResponseDTO;
import com.hanaro.hanaconnect.entity.House;
import com.hanaro.hanaconnect.entity.Member;
import com.hanaro.hanaconnect.entity.PhoneName;
import com.hanaro.hanaconnect.entity.Transaction;
import com.hanaro.hanaconnect.repository.HouseRepository;
import com.hanaro.hanaconnect.repository.MemberRepository;
import com.hanaro.hanaconnect.repository.PhoneNameRepository;
import com.hanaro.hanaconnect.repository.RelationRepository;
import com.hanaro.hanaconnect.repository.TransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HouseService {

	private final HouseRepository houseRepository;
	private final MemberRepository memberRepository;
	private final RelationRepository relationRepository;
	private final PhoneNameRepository phoneNameRepository;
	private final TransactionRepository transactionRepository;

	public HouseStatusResponseDTO getHouseStatus(Long requesterId, Long kidId) {
		Member requester = findRequester(requesterId);
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
		int totalCount = safeTotalCount(house);
		int level = HouseLevelCalculator.calculateLevel(totalCount);
		int gauge = HouseLevelCalculator.calculateGauge(totalCount);
		HouseLevel houseLevel = HouseLevel.from(level);

		String message = buildMessage(requester, kid, houseLevel, house, totalCount);

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

	public HouseHistoryResponseDTO getHouseHistory(Long requesterId, Long kidId) {
		HouseContext context = resolveHouseContext(requesterId, kidId);

		List<Transaction> transactions = transactionRepository
			.findByReceiverAccountIdAndTransactionTypeOrderByCreatedAtAsc(
				context.house().getAccount().getId(),
				TransactionType.SUBSCRIPTION
			);

		List<HouseHistoryItemDTO> histories = buildHistories(context.house(), transactions);

		return HouseHistoryResponseDTO.builder()
			.histories(histories)
			.build();
	}

	private Member findRequester(Long requesterId) {
		return memberRepository.findById(requesterId)
			.orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다."));
	}

	private HouseContext resolveHouseContext(Long requesterId, Long kidId) {
		Member requester = findRequester(requesterId);
		Member kid = resolveTargetKid(requester, kidId);
		House house = houseRepository.findByMemberId(kid.getId())
			.orElseThrow(() -> new EntityNotFoundException("청약 정보를 찾을 수 없습니다."));
		return new HouseContext(requester, kid, house);
	}

	private int safeTotalCount(House house) {
		return house.getTotalCount() != null ? house.getTotalCount() : 0;
	}

	private Member resolveTargetKid(Member requester, Long kidId) {
		if (requester.getMemberRole() == MemberRole.KID) {
			return requester;
		}

		if (kidId == null) {
			throw new IllegalArgumentException("kidId는 필수입니다.");
		}

		Member kid = memberRepository.findById(kidId)
			.orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다."));

		boolean hasRelation = relationRepository
			.existsByMember_IdAndConnectMember_IdAndConnectMemberRole(
				requester.getId(),
				kidId,
				MemberRole.KID
			);

		if (!hasRelation || kid.getMemberRole() != MemberRole.KID) {
			throw new AccessDeniedException("해당 아이의 정보에 접근할 수 없습니다.");
		}

		return kid;
	}

	private String buildMessage(Member requester, Member kid, HouseLevel houseLevel, House house, int totalCount) {
		if (requester.getMemberRole() == MemberRole.PARENT) {
			return houseLevel.getDefaultMessage(totalCount);
		}

		Optional<Transaction> latestPaymentOpt = transactionRepository
			.findTopByReceiverAccountIdAndTransactionTypeOrderByCreatedAtDesc(
				house.getAccount().getId(),
				TransactionType.SUBSCRIPTION
			);

		if (latestPaymentOpt.isEmpty()) {
			return houseLevel.getDefaultMessage(totalCount);
		}

		Transaction latestPayment = latestPaymentOpt.get();

		if (latestPayment.getSenderAccount() == null ||
			latestPayment.getSenderAccount().getMember() == null) {
			return houseLevel.getDefaultMessage(totalCount);
		}

		Member payer = latestPayment.getSenderAccount().getMember();

		String payerDisplayName = phoneNameRepository
			.findByWhoIdAndWhomId(kid.getId(), payer.getId())
			.map(PhoneName::getWhomName)
			.orElse(payer.getName());

		return houseLevel.getPersonalizedMessage(payerDisplayName, totalCount);
	}

	private List<HouseHistoryItemDTO> buildHistories(House house, List<Transaction> transactions) {
		return IntStream.range(0, transactions.size())
			.mapToObj(index -> toHistoryItem(house, transactions.get(index), index + 1))
			.flatMap(Optional::stream)
			.sorted((a, b) -> b.getPaidAt().compareTo(a.getPaidAt()))
			.collect(Collectors.toList());
	}

	private Optional<HouseHistoryItemDTO> toHistoryItem(House house, Transaction transaction, int totalCount) {
		boolean isFirst = totalCount == 1;
		boolean isYearlyMilestone = totalCount % 12 == 0;

		if (!isFirst && !isYearlyMilestone) {
			return Optional.empty();
		}

		int year = isFirst ? 0 : totalCount / 12;
		int level = HouseLevelCalculator.calculateLevel(totalCount);
		HouseLevel houseLevel = HouseLevel.from(level);

		return Optional.of(
			HouseHistoryItemDTO.builder()
				.year(year)
				.level(level)
				.totalCount(totalCount)
				.paidAt(transaction.getCreatedAt().toLocalDate())
				.isFirst(isFirst)
				.reward(houseLevel.getChangeDescription())
				.build()
		);
	}

	private record HouseContext(
		Member requester,
		Member kid,
		House house
	) {
	}
}
