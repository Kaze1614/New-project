package com.datong.mathai.review;

import com.datong.mathai.common.AppException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final JdbcTemplate jdbcTemplate;

    public ReviewService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ReviewTaskView> list(Long userId) {
        String sql = """
            SELECT r.id, r.mistake_id, r.due_date, r.completed, r.completed_at,
                   m.question_title, m.question_content
            FROM review_tasks r
            JOIN mistake_records m ON r.mistake_id = m.id
            WHERE r.user_id = ?
            ORDER BY r.completed ASC, r.due_date ASC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ReviewTaskView(
            rs.getLong("id"),
            rs.getLong("mistake_id"),
            rs.getString("question_title"),
            rs.getString("question_content"),
            rs.getTimestamp("due_date").toLocalDateTime(),
            rs.getBoolean("completed"),
            rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime()
        ), userId);
    }

    public ReviewTaskView complete(Long userId, Long taskId) {
        LocalDateTime now = LocalDateTime.now();
        int affected = jdbcTemplate.update(
            "UPDATE review_tasks SET completed = 1, completed_at = ?, updated_at = ? WHERE id = ? AND user_id = ?",
            Timestamp.valueOf(now),
            Timestamp.valueOf(now),
            taskId,
            userId
        );
        if (affected == 0) {
            throw new AppException(404, "Review task not found");
        }

        var mistakeIds = jdbcTemplate.query(
            "SELECT mistake_id FROM review_tasks WHERE id = ? AND user_id = ?",
            (rs, rowNum) -> rs.getLong("mistake_id"),
            taskId,
            userId
        );
        if (!mistakeIds.isEmpty()) {
            jdbcTemplate.update(
                "UPDATE mistake_records SET status = 'MASTERED', updated_at = ? WHERE id = ?",
                Timestamp.valueOf(now),
                mistakeIds.get(0)
            );
        }

        var rows = jdbcTemplate.query(
            """
                SELECT r.id, r.mistake_id, r.due_date, r.completed, r.completed_at, m.question_title, m.question_content
                FROM review_tasks r
                JOIN mistake_records m ON r.mistake_id = m.id
                WHERE r.id = ? AND r.user_id = ?
                """,
            (rs, rowNum) -> new ReviewTaskView(
                rs.getLong("id"),
                rs.getLong("mistake_id"),
                rs.getString("question_title"),
                rs.getString("question_content"),
                rs.getTimestamp("due_date").toLocalDateTime(),
                rs.getBoolean("completed"),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime()
            ),
            taskId,
            userId
        );

        if (rows.isEmpty()) {
            throw new AppException(404, "Review task not found");
        }
        return rows.get(0);
    }
}
