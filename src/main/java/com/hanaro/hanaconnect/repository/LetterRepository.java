package com.hanaro.hanaconnect.repository;

import com.hanaro.hanaconnect.dto.RelayHistoryDTO;
import com.hanaro.hanaconnect.entity.Letter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LetterRepository extends JpaRepository<Letter, Long> {

	@Query("SELECT new com.hanaro.hanaconnect.dto.RelayHistoryDTO(" +
		"t.id, t.createdAt, t.transactionMoney, l.content) " +
		"FROM Letter l " +
		"JOIN l.transaction t " +
		"WHERE t.senderAccount.member.id = :memberId " +
		"AND t.receiverAccount.id = :targetAccountId " +
		"ORDER BY t.createdAt DESC")
	List<RelayHistoryDTO> findMyRelayHistory(@Param("memberId") Long memberId,
		@Param("targetAccountId") Long targetAccountId);}
