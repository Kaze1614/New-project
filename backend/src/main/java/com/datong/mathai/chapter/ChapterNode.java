package com.datong.mathai.chapter;

import java.util.ArrayList;
import java.util.List;

public record ChapterNode(Long id, String title, Integer sortOrder, List<ChapterNode> children) {
    public static ChapterNode of(Long id, String title, Integer sortOrder) {
        return new ChapterNode(id, title, sortOrder, new ArrayList<>());
    }
}
