package com.datong.mathai.favorite;

import java.util.List;
import java.time.LocalDateTime;

public record FavoriteItem(
    Long id,
    Long questionId,
    Long chapterId,
    String difficulty,
    String title,
    String content,
    String questionType,
    List<String> options,
    String correctAnswer,
    String explanation,
    String sourceLabel,
    LocalDateTime createdAt
) {
}
