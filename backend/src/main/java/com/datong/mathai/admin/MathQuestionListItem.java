package com.datong.mathai.admin;

import java.time.LocalDateTime;

public record MathQuestionListItem(
    Long id,
    Integer questionNo,
    String bookName,
    String chapterName,
    String sectionName,
    String contentPreview,
    Integer sourceYear,
    String sourcePaper,
    String sourceLabel,
    LocalDateTime updatedAt
) {
}
