package com.hanaro.hanaconnect.dto.transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecentTransferResponseDTO {
	private LocalDateTime transactionDate; // 송금 날짜
	private BigDecimal amount;            // 송금 금액
}
