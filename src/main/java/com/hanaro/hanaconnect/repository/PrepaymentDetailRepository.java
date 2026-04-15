package com.hanaro.hanaconnect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hanaro.hanaconnect.entity.PrepaymentDetail;

public interface PrepaymentDetailRepository extends JpaRepository<PrepaymentDetail, Long> {

	@Query("select max(p.roundNo) from PrepaymentDetail p where p.account.id = :accountId")
	Optional<Integer> findMaxRoundNoByAccountId(@Param("accountId") Long accountId);

}
