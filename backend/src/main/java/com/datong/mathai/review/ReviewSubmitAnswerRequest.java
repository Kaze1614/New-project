package com.datong.mathai.review;

import jakarta.validation.constraints.NotNull;

public record ReviewSubmitAnswerRequest(
    @NotNull(message = "taskId is required")
    Long taskId,
    String answer
) {
}
