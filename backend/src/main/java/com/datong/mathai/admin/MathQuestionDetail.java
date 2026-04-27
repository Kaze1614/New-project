package com.datong.mathai.admin;

import com.datong.mathai.question.QuestionOptionPayload;
import com.datong.mathai.question.QuestionSubQuestionPayload;

import java.time.LocalDateTime;
import java.util.List;

public record MathQuestionDetail(
    Long id,
    String imageUrl,
    String questionType,
    String rawTextLatex,
    List<QuestionOptionPayload> options,
    List<String> answers,
    List<QuestionSubQuestionPayload> subQuestions,
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
