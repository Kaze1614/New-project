package com.datong.mathai.review;

import java.time.LocalDateTime;

public record ReviewTaskView(
    Long id,
    Long mistakeId,
    String questionTitle,
    String questionContent,
    LocalDateTime dueDate,
    boolean completed,
    LocalDateTime completedAt
) {
}
