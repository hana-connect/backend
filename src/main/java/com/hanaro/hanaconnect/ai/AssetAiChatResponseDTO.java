package com.hanaro.hanaconnect.ai;

import java.util.List;

public record AssetAiChatResponseDTO(List<Choice> choices) {

	public record Choice(Message message) {}

	public record Message(String content) {}
}
