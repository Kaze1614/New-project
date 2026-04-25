package com.datong.mathai.review;

public record ReviewSubmitItemResult(
    Long taskId,
    boolean answered,
    boolean correct,
    String correctAnswer,
    String explanation,
    int nextBox,
    boolean removedFromMistakes
) {
}
