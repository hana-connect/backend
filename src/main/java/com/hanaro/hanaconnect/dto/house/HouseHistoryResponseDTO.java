package com.hanaro.hanaconnect.dto.house;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HouseHistoryResponseDTO {
	private List<HouseHistoryItemDTO> histories;
}
