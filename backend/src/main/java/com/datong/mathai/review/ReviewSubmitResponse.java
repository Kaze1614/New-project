package com.datong.mathai.review;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewSubmitResponse(
    LocalDateTime submittedAt,
    int totalCount,
    int answeredCount,
    int correctCount,
    List<ReviewSubmitItemResult> items
) {
}
