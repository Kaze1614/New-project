package com.datong.mathai.study;

import com.datong.mathai.question.QuestionSubQuestionPayload;

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
    String studentAnswerText,
    boolean answered,
    Boolean correct,
    String sourceLabel,
    String sourceSnapshotPath,
    String explanationSource,
    String explanationReviewStatus,
    List<QuestionSubQuestionPayload> subQuestions,
    String officialAnswer,
    String officialExplanation,
    LocalDateTime answeredAt
) {
}
