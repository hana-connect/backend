package com.hanaro.hanaconnect.dto.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAnswerRequestDTO {

	@NotNull(message = "선택한 답은 필수입니다.")
	@Min(value = 0, message = "선택 인덱스는 0 이상이어야 합니다.")
	@Max(value = 3, message = "선택 인덱스는 3 이하여야 합니다.")
	private Integer selectedIndex;

}
