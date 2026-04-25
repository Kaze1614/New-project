package com.datong.mathai.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReviewSubmitRequest(
    @NotNull(message = "answers is required")
    @Valid
    List<ReviewSubmitAnswerRequest> answers
) {
}
