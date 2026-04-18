package com.hanaro.hanaconnect.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SenderInfoDTO {
	private Long senderId;
	private String senderName;
}
