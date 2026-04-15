package com.hanaro.hanaconnect.repository;

import com.hanaro.hanaconnect.dto.RelayHistoryDTO;
import com.hanaro.hanaconnect.dto.SavingsTransactionDTO;
import com.hanaro.hanaconnect.dto.SenderInfoDTO;
import com.hanaro.hanaconnect.entity.Letter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LetterRepository extends JpaRepository<Letter, Long> {

	// 부모용
	@Query("SELECT new com.hanaro.hanaconnect.dto.RelayHistoryDTO(" +
		"t.id, t.createdAt, t.transactionMoney, l.content) " +
		"FROM Letter l " +
		"JOIN l.transaction t " +
		"WHERE t.senderAccount.member.id = :memberId " +
		"AND t.receiverAccount.id = :targetAccountId " +
		"AND t.transactionType = com.hanaro.hanaconnect.common.enums.TransactionType.SAVINGS_DEPOSIT " +
		"ORDER BY t.createdAt ASC")
	Page<RelayHistoryDTO> findMyRelayHistory(
		@Param("memberId") Long memberId,
		@Param("targetAccountId") Long targetAccountId,
		Pageable pageable
	);

	// 최근 3개만 가져오는 메서드
	@Query("SELECT new com.hanaro.hanaconnect.dto.RelayHistoryDTO(" +
		"t.id, t.createdAt, t.transactionMoney, l.content) " +
		"FROM Letter l " +
		"JOIN l.transaction t " +
		"WHERE t.senderAccount.member.id = :memberId " +
		"AND t.receiverAccount.id = :targetAccountId " +
		"ORDER BY t.createdAt DESC")
	List<RelayHistoryDTO> findTop3RelayHistory(@Param("memberId") Long memberId,
		@Param("targetAccountId") Long targetAccountId,
		org.springframework.data.domain.Pageable pageable);

	// 아이용
	@Query("SELECT new com.hanaro.hanaconnect.dto.SavingsTransactionDTO(" +
		"t.id, t.createdAt, t.transactionMoney, t.transactionBalance, l.content, m.name, m.id) " +
		"FROM Transaction t " +
		"JOIN Letter l ON l.transaction = t " +
		"JOIN t.senderAccount sa " +
		"JOIN sa.member m " +
		"WHERE t.receiverAccount.id = :accountId " +
		"AND (:senderId IS NULL OR m.id = :senderId) " +
		"AND t.transactionType = com.hanaro.hanaconnect.common.enums.TransactionType.SAVINGS_DEPOSIT " +
		"ORDER BY t.createdAt ASC")
	Page<SavingsTransactionDTO> findAllSavingsDetails(
		@Param("accountId") Long accountId,
		@Param("senderId") Long senderId,
		Pageable pageable
	);

	@Query("SELECT DISTINCT new com.hanaro.hanaconnect.dto.SenderInfoDTO(m.id, m.name) " +
		"FROM Transaction t " +
		"JOIN t.senderAccount sa " +
		"JOIN sa.member m " +
		"WHERE t.receiverAccount.id = :accountId " +
		"AND t.transactionType = com.hanaro.hanaconnect.common.enums.TransactionType.SAVINGS_DEPOSIT")
	List<SenderInfoDTO> findDistinctSendersByAccountId(@Param("accountId") Long accountId);
}
