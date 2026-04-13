package com.datong.mathai.review;

import com.datong.mathai.common.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ReviewService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ReviewService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ReviewTaskView> list(Long userId) {
        return jdbcTemplate.query(baseSql() + " ORDER BY r.suspended ASC, r.completed ASC, r.due_date ASC, r.id ASC",
            (rs, rowNum) -> mapView(rs),
            userId);
    }

    public ReviewTaskView next(Long userId) {
        List<ReviewTaskView> dueRows = jdbcTemplate.query(
            baseSql() + " AND r.completed = 0 AND r.suspended = 0 AND r.due_date <= ? ORDER BY r.due_date ASC, r.id ASC LIMIT 1",
            (rs, rowNum) -> mapView(rs),
            userId,
            Timestamp.valueOf(LocalDateTime.now())
        );
        if (!dueRows.isEmpty()) {
            return dueRows.get(0);
        }

        List<ReviewTaskView> upcomingRows = jdbcTemplate.query(
            baseSql() + " AND r.completed = 0 AND r.suspended = 0 ORDER BY r.due_date ASC, r.id ASC LIMIT 1",
            (rs, rowNum) -> mapView(rs),
            userId
        );
        return upcomingRows.isEmpty() ? null : upcomingRows.get(0);
    }

    public ReviewTaskView complete(Long userId, Long taskId) {
        return applyGrade(userId, taskId, "easy");
    }

    public ReviewTaskView rate(Long userId, Long taskId, String grade) {
        return applyGrade(userId, taskId, grade);
    }

    private ReviewTaskView applyGrade(Long userId, Long taskId, String gradeRaw) {
        if (gradeRaw == null || gradeRaw.isBlank()) {
            throw new AppException(400, "grade is required");
        }
        String grade = gradeRaw.trim().toLowerCase(Locale.ROOT);
        if (!grade.equals("again") && !grade.equals("hard") && !grade.equals("easy")) {
            throw new AppException(400, "grade must be again|hard|easy");
        }

        TaskMeta task = findTaskMeta(userId, taskId);
        LocalDateTime now = LocalDateTime.now();

        int repetition = task.repetition();
        int intervalDays = task.intervalDays();
        double easeFactor = task.easeFactor();
        boolean completed = false;
        boolean suspended = false;
        LocalDateTime completedAt = null;
        LocalDateTime dueDate;
        String mistakeStatus;

        switch (grade) {
            case "again" -> {
                repetition = 0;
                intervalDays = 1;
                easeFactor = Math.max(1.3d, easeFactor - 0.2d);
                dueDate = now.plusDays(1);
                mistakeStatus = "REVIEWING";
            }
            case "hard" -> {
                repetition = repetition + 1;
                intervalDays = 3;
                easeFactor = Math.max(1.3d, easeFactor - 0.05d);
                dueDate = now.plusDays(3);
                mistakeStatus = "REVIEWING";
            }
            default -> {
                repetition = repetition + 1;
                intervalDays = Math.max(intervalDays, 7);
                easeFactor = Math.min(3.0d, easeFactor + 0.1d);
                dueDate = now.plusDays(30);
                completed = true;
                suspended = true;
                completedAt = now;
                mistakeStatus = "MASTERED";
            }
        }

        int affected = jdbcTemplate.update(
            """
                UPDATE review_tasks
                SET due_date = ?, completed = ?, suspended = ?, repetition = ?, interval_days = ?, ease_factor = ?, last_grade = ?, completed_at = ?, updated_at = ?
                WHERE id = ? AND user_id = ?
                """,
            Timestamp.valueOf(dueDate),
            completed ? 1 : 0,
            suspended ? 1 : 0,
            repetition,
            intervalDays,
            easeFactor,
            grade.toUpperCase(Locale.ROOT),
            completedAt == null ? null : Timestamp.valueOf(completedAt),
            Timestamp.valueOf(now),
            taskId,
            userId
        );
        if (affected == 0) {
            throw new AppException(404, "Review task not found");
        }

        jdbcTemplate.update(
            "UPDATE mistake_records SET status = ?, updated_at = ? WHERE id = ? AND user_id = ?",
            mistakeStatus,
            Timestamp.valueOf(now),
            task.mistakeId(),
            userId
        );

        return getTaskById(userId, taskId);
    }

    private TaskMeta findTaskMeta(Long userId, Long taskId) {
        List<TaskMeta> rows = jdbcTemplate.query(
            "SELECT id, mistake_id, repetition, interval_days, ease_factor FROM review_tasks WHERE id = ? AND user_id = ?",
            (rs, rowNum) -> new TaskMeta(
                rs.getLong("id"),
                rs.getLong("mistake_id"),
                rs.getInt("repetition"),
                rs.getInt("interval_days"),
                rs.getDouble("ease_factor")
            ),
            taskId,
            userId
        );
        if (rows.isEmpty()) {
            throw new AppException(404, "Review task not found");
        }
        return rows.get(0);
    }

    private ReviewTaskView getTaskById(Long userId, Long taskId) {
        List<ReviewTaskView> rows = jdbcTemplate.query(
            baseSql() + " AND r.id = ?",
            (rs, rowNum) -> mapView(rs),
            userId,
            taskId
        );
        if (rows.isEmpty()) {
            throw new AppException(404, "Review task not found");
        }
        return rows.get(0);
    }

    private String baseSql() {
        return """
            SELECT r.id, r.mistake_id, r.due_date, r.completed, r.suspended, r.repetition, r.interval_days, r.ease_factor, r.last_grade, r.completed_at,
                   m.question_id, m.chapter_id, m.difficulty, m.question_title, m.question_content,
                   q.answer_json, q.explanation
            FROM review_tasks r
            JOIN mistake_records m ON r.mistake_id = m.id
            LEFT JOIN questions q ON q.id = m.question_id
            WHERE r.user_id = ?
            """;
    }

    private ReviewTaskView mapView(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ReviewTaskView(
            rs.getLong("id"),
            rs.getLong("mistake_id"),
            rs.getObject("question_id") == null ? null : rs.getLong("question_id"),
            rs.getObject("chapter_id") == null ? null : rs.getLong("chapter_id"),
            rs.getString("difficulty"),
            rs.getString("question_title"),
            rs.getString("question_content"),
            parseAnswerForDisplay(rs.getString("answer_json")),
            rs.getString("explanation"),
            rs.getTimestamp("due_date").toLocalDateTime(),
            rs.getBoolean("completed"),
            rs.getBoolean("suspended"),
            rs.getInt("repetition"),
            rs.getInt("interval_days"),
            rs.getDouble("ease_factor"),
            rs.getString("last_grade"),
            rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime()
        );
    }

    private String parseAnswerForDisplay(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            List<String> values = objectMapper.readValue(rawJson, new TypeReference<>() {
            });
            if (values.isEmpty()) {
                return null;
            }
            return String.join(", ", values);
        } catch (Exception ignored) {
            return rawJson;
        }
    }

    private record TaskMeta(Long id, Long mistakeId, int repetition, int intervalDays, double easeFactor) {
    }
}
