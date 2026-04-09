package com.datong.mathai.mistake;

import java.util.List;

public record MistakeAnalysisView(
    List<String> knowledgePoints,
    String errorType,
    List<String> solvingSteps,
    List<String> variants,
    List<String> followUpSuggestions
) {
}
