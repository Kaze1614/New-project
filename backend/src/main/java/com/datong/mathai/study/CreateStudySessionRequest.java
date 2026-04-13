package com.datong.mathai.study;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateStudySessionRequest(
    Long chapterId,
    @Min(value = 300, message = "durationSeconds must be >= 300")
    @Max(value = 10800, message = "durationSeconds must be <= 10800")
    Integer durationSeconds
) {
}
