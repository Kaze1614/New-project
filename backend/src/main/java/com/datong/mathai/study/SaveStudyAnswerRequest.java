package com.datong.mathai.study;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveStudyAnswerRequest(
    @NotNull(message = "itemId is required")
    Long itemId,
    @NotBlank(message = "answer is required")
    @Size(max = 500, message = "answer must be <= 500 characters")
    String answer
) {
}
