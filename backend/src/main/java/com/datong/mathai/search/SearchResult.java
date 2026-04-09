package com.datong.mathai.search;

import java.util.List;

public record SearchResult(
    List<SearchChapterItem> chapters,
    List<SearchQuestionItem> questions,
    List<SearchMistakeItem> mistakes
) {
}
