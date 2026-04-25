package com.datong.mathai.admin;

import java.time.LocalDateTime;

public record MathQuestionDetail(
    Long id,
    String imageUrl,
    String rawTextLatex,
    String answerLatex,
    String teacherExplanation,
    String bookName,
    String chapterName,
    String sectionName,
    Integer sourceYear,
    String sourcePaper,
    Integer questionNo,
    String sourceLabel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
