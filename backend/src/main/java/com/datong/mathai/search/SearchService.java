package com.datong.mathai.search;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class SearchService {

    private final JdbcTemplate jdbcTemplate;

    public SearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SearchResult search(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new SearchResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        String like = "%" + keyword.trim() + "%";

        var chapters = jdbcTemplate.query(
            "SELECT id, title FROM chapters WHERE title LIKE ? ORDER BY sort_order ASC LIMIT 10",
            (rs, rowNum) -> new SearchChapterItem(rs.getLong("id"), rs.getString("title")),
            like
        );

        var questions = jdbcTemplate.query(
            "SELECT id, title, content FROM questions WHERE import_status = 'READY' AND (title LIKE ? OR content LIKE ?) ORDER BY id DESC LIMIT 20",
            (rs, rowNum) -> new SearchQuestionItem(rs.getLong("id"), rs.getString("title"), rs.getString("content")),
            like,
            like
        );

        var mistakes = jdbcTemplate.query(
            """
                SELECT id, question_title, question_content, status
                FROM mistake_records
                WHERE user_id = ? AND (question_title LIKE ? OR question_content LIKE ?)
                ORDER BY updated_at DESC LIMIT 20
                """,
            (rs, rowNum) -> new SearchMistakeItem(
                rs.getLong("id"),
                rs.getString("question_title"),
                rs.getString("question_content"),
                rs.getString("status")
            ),
            userId,
            like,
            like
        );

        return new SearchResult(chapters, questions, mistakes);
    }
}
