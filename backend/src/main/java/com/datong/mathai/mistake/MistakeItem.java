package com.datong.mathai.mistake;

import java.time.LocalDateTime;

public record MistakeItem(
    Long id,
    Long questionId,
    Long chapterId,
    String difficulty,
    String questionTitle,
    String questionContent,
    String imageUrl,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    MistakeAnalysisView analysis
) {
}
