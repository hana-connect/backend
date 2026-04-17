package com.hanaro.hanaconnect.common.converter;

import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (Exception e) {
			throw new RuntimeException("JSON 변환 실패", e);
		}
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		try {
			return objectMapper.readValue(dbData, new TypeReference<>() {});
		} catch (Exception e) {
			throw new RuntimeException("JSON 파싱 실패", e);
		}
	}
}
