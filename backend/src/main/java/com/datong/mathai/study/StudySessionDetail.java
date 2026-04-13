package com.datong.mathai.study;

import java.time.LocalDateTime;
import java.util.List;

public record StudySessionDetail(
    Long id,
    Long chapterId,
    int durationSeconds,
    LocalDateTime startedAt,
    LocalDateTime submittedAt,
    boolean submitted,
    int totalCount,
    int answeredCount,
    int correctCount,
    List<StudySessionItemView> items
) {
}
