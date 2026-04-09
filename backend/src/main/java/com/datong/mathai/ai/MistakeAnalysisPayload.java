package com.datong.mathai.ai;

import java.util.List;

public record MistakeAnalysisPayload(
    List<String> knowledgePoints,
    String errorType,
    List<String> solvingSteps,
    List<String> variants,
    List<String> followUpSuggestions
) {
}
