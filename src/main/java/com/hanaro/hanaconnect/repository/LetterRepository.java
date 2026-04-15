package com.hanaro.hanaconnect.repository;

import com.hanaro.hanaconnect.dto.RelayHistoryDTO;
import com.hanaro.hanaconnect.entity.Letter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LetterRepository extends JpaRepository<Letter, Long> {

	@Query("SELECT new com.hanaro.hanaconnect.dto.RelayHistoryDTO(" +
		"t.id, m.name, t.createdAt, t.transactionMoney, l.content) " +
		"FROM Letter l " +
		"JOIN l.transaction t " +
		"JOIN t.senderAccount sa " + // 보낸 계좌 조인
		"JOIN sa.member m " + // 보낸 계좌의 회원 조인
		"WHERE t.senderAccount.member.id = :memberId " +
		"AND t.receiverAccount.id = :targetAccountId " +
		"ORDER BY t.createdAt DESC")
	List<RelayHistoryDTO> findMyRelayHistory(@Param("memberId") Long memberId,
		@Param("targetAccountId") Long targetAccountId);

	// 최근 3개만 가져오는 메서드
	@Query("SELECT new com.hanaro.hanaconnect.dto.RelayHistoryDTO(" +
		"t.id, m.name, t.createdAt, t.transactionMoney, l.content) " +
		"FROM Letter l " +
		"JOIN l.transaction t " +
		"JOIN t.senderAccount sa " +
		"JOIN sa.member m " +
		"WHERE t.senderAccount.member.id = :memberId " +
		"AND t.receiverAccount.id = :targetAccountId " +
		"ORDER BY t.createdAt DESC")
	List<RelayHistoryDTO> findTop3RelayHistory(@Param("memberId") Long memberId,
		@Param("targetAccountId") Long targetAccountId,
		org.springframework.data.domain.Pageable pageable);


}
