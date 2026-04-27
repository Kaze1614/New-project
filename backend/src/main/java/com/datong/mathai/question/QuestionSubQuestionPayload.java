package com.datong.mathai.question;

import java.util.List;

public record QuestionSubQuestionPayload(
    Integer index,
    String prompt,
    String referenceAnswer,
    List<String> steps
) {
}
