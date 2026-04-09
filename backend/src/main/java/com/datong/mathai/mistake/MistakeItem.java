package com.datong.mathai.mistake;

import java.time.LocalDateTime;

public record MistakeItem(
    Long id,
    Long chapterId,
    String questionTitle,
    String questionContent,
    String imageUrl,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    MistakeAnalysisView analysis
) {
}
