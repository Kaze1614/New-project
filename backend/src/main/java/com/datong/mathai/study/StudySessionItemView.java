package com.datong.mathai.study;

import java.time.LocalDateTime;
import java.util.List;

public record StudySessionItemView(
    Long itemId,
    Long questionId,
    int sortOrder,
    String type,
    String difficulty,
    String title,
    String content,
    List<String> options,
    String userAnswer,
    boolean answered,
    Boolean correct,
    String officialAnswer,
    String officialExplanation,
    LocalDateTime answeredAt
) {
}
