package com.datong.mathai.review;

import jakarta.validation.constraints.NotBlank;

public record ReviewRateRequest(
    @NotBlank(message = "grade is required")
    String grade
) {
}
