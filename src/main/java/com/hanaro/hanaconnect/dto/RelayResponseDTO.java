package com.hanaro.hanaconnect.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelayResponseDTO {
	private String productNickname;
	private String accountNumber;
	private List<RelayHistoryDTO> history;
}
