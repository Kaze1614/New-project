package com.datong.mathai.dashboard;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardOverview overview(Long userId) {
        int questionBankTotal = queryInt("SELECT COUNT(1) FROM questions WHERE import_status = 'READY'");
        int totalMistakes = queryInt("SELECT COUNT(1) FROM mistake_records WHERE user_id = ?", userId);
        int mastered = queryInt("SELECT COUNT(1) FROM mistake_records WHERE user_id = ? AND status = 'MASTERED'", userId);
        int pendingReview = queryInt("SELECT COUNT(1) FROM review_tasks WHERE user_id = ? AND completed = 0 AND suspended = 0", userId);
        int criticalReviewCount = queryInt(
            "SELECT COUNT(1) FROM review_tasks WHERE user_id = ? AND completed = 0 AND suspended = 0 AND due_date <= ?",
            userId,
            LocalDateTime.now()
        );

        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        int todayCompleted = queryInt(
            "SELECT COUNT(1) FROM review_tasks WHERE user_id = ? AND completed = 1 AND completed_at >= ? AND completed_at < ?",
            userId,
            start,
            end
        );

        int totalSolved = queryInt(
            "SELECT COUNT(1) FROM study_session_items si JOIN study_sessions ss ON si.session_id = ss.id WHERE ss.user_id = ? AND si.answered_at IS NOT NULL",
            userId
        );
        int correctSolved = queryInt(
            "SELECT COUNT(1) FROM study_session_items si JOIN study_sessions ss ON si.session_id = ss.id WHERE ss.user_id = ? AND si.is_correct = 1",
            userId
        );
        double accuracyRate = totalSolved == 0 ? 0d : (correctSolved * 100.0 / totalSolved);

        return new DashboardOverview(
            questionBankTotal,
            totalMistakes,
            mastered,
            pendingReview,
            todayCompleted,
            totalSolved,
            Math.round(accuracyRate * 100.0) / 100.0,
            criticalReviewCount,
            queryWeakSpotHint(userId)
        );
    }

    private int queryInt(String sql, Object... params) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return result == null ? 0 : result;
    }

    private String queryWeakSpotHint(Long userId) {
        var rows = jdbcTemplate.query(
            """
                SELECT c.title AS chapter_title, COUNT(1) AS cnt
                FROM mistake_records m
                LEFT JOIN chapters c ON c.id = m.chapter_id
                WHERE m.user_id = ?
                GROUP BY c.title
                ORDER BY cnt DESC
                LIMIT 1
                """,
            (rs, rowNum) -> {
                String chapterTitle = rs.getString("chapter_title");
                if (chapterTitle == null || chapterTitle.isBlank()) {
                    chapterTitle = "综合题";
                }
                return chapterTitle + " · " + rs.getInt("cnt") + "题";
            },
            userId
        );
        if (!rows.isEmpty()) {
            return rows.get(0);
        }
        return "今日记录 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd")) + "，继续保持。";
    }
}
