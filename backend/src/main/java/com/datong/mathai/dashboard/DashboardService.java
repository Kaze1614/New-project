package com.datong.mathai.dashboard;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardOverview overview(Long userId) {
        int totalMistakes = queryInt("SELECT COUNT(1) FROM mistake_records WHERE user_id = ?", userId);
        int mastered = queryInt("SELECT COUNT(1) FROM mistake_records WHERE user_id = ? AND status = 'MASTERED'", userId);
        int pendingReview = queryInt("SELECT COUNT(1) FROM review_tasks WHERE user_id = ? AND completed = 0", userId);
        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        int todayCompleted = queryInt(
            "SELECT COUNT(1) FROM review_tasks WHERE user_id = ? AND completed = 1 AND completed_at >= ? AND completed_at < ?",
            userId,
            start,
            end
        );
        return new DashboardOverview(totalMistakes, mastered, pendingReview, todayCompleted);
    }

    private int queryInt(String sql, Object... params) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return result == null ? 0 : result;
    }
}
