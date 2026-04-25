package com.datong.mathai.review;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewTaskView(
    Long id,
    Long mistakeId,
    Long questionId,
    Long chapterId,
    String difficulty,
    String questionTitle,
    String questionContent,
    String type,
    List<String> options,
    String sourceLabel,
    String sourceSnapshotPath,
    String explanationSource,
    String explanationReviewStatus,
    String officialAnswer,
    String officialExplanation,
    LocalDateTime dueDate,
    boolean completed,
    boolean suspended,
    int repetition,
    int intervalDays,
    double easeFactor,
    String lastGrade,
    LocalDateTime completedAt
) {
}
