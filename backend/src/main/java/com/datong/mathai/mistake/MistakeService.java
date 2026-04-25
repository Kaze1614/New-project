package com.datong.mathai.mistake;

import com.datong.mathai.ai.AIProvider;
import com.datong.mathai.ai.MistakeAnalysisPayload;
import com.datong.mathai.common.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class MistakeService {

    private final JdbcTemplate jdbcTemplate;
    private final AIProvider aiProvider;
    private final ObjectMapper objectMapper;

    public MistakeService(JdbcTemplate jdbcTemplate, AIProvider aiProvider, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiProvider = aiProvider;
        this.objectMapper = objectMapper;
    }

    public List<MistakeItem> list(Long userId, Long chapterId, String difficulty, String keyword) {
        StringBuilder sql = new StringBuilder("""
            SELECT m.id, m.question_id, m.chapter_id, m.difficulty, m.question_title, m.question_content, m.image_url, m.status, m.created_at, m.updated_at,
                   a.error_type, a.knowledge_points_json, a.solving_steps_json, a.variants_json, a.follow_up_json
            FROM mistake_records m
            LEFT JOIN mistake_analysis a ON m.id = a.mistake_id
            WHERE m.user_id = ?
            """);
        List<Object> args = new ArrayList<>();
        args.add(userId);

        if (chapterId != null) {
            sql.append(" AND m.chapter_id = ?");
            args.add(chapterId);
        }
        if (difficulty != null && !difficulty.isBlank()) {
            sql.append(" AND m.difficulty = ?");
            args.add(difficulty.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (m.question_title LIKE ? OR m.question_content LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY m.created_at DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            MistakeAnalysisView analysisView = null;
            if (rs.getString("error_type") != null) {
                analysisView = new MistakeAnalysisView(
                    parseList(rs.getString("knowledge_points_json")),
                    rs.getString("error_type"),
                    parseList(rs.getString("solving_steps_json")),
                    parseList(rs.getString("variants_json")),
                    parseList(rs.getString("follow_up_json"))
                );
            }

            return new MistakeItem(
                rs.getLong("id"),
                rs.getObject("question_id") == null ? null : rs.getLong("question_id"),
                rs.getObject("chapter_id") == null ? null : rs.getLong("chapter_id"),
                rs.getString("difficulty"),
                rs.getString("question_title"),
                rs.getString("question_content"),
                rs.getString("image_url"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                analysisView
            );
        }, args.toArray());
    }

    public MistakeItem create(Long userId, CreateMistakeRequest request) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO mistake_records(user_id, question_id, chapter_id, difficulty, question_title, question_content, image_url, status, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setObject(2, null);
            if (request.chapterId() == null) {
                ps.setObject(3, null);
            } else {
                ps.setLong(3, request.chapterId());
            }
            ps.setObject(4, null);
            ps.setString(5, request.questionTitle());
            ps.setString(6, request.questionContent());
            ps.setString(7, request.imageUrl());
            ps.setString(8, "NEW");
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.setTimestamp(10, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long id = extractId(keyHolder, "Create mistake failed");
        return new MistakeItem(id, null, request.chapterId(), null, request.questionTitle(), request.questionContent(), request.imageUrl(), "NEW", now, now, null);
    }

    public void delete(Long userId, Long id) {
        int affected = jdbcTemplate.update("DELETE FROM mistake_records WHERE id = ? AND user_id = ?", id, userId);
        if (affected == 0) {
            throw new AppException(404, "Mistake not found");
        }
    }

    public MistakeAnalysisView analyze(Long userId, Long mistakeId) {
        var rows = jdbcTemplate.query(
            "SELECT id, question_title, question_content FROM mistake_records WHERE id = ? AND user_id = ?",
            (rs, rowNum) -> new MistakeRow(rs.getLong("id"), rs.getString("question_title"), rs.getString("question_content")),
            mistakeId,
            userId
        );
        if (rows.isEmpty()) {
            throw new AppException(404, "Mistake not found");
        }

        MistakeRow row = rows.get(0);
        MistakeAnalysisPayload payload = aiProvider.analyze(row.title(), row.content());
        String kp = writeList(payload.knowledgePoints());
        String steps = writeList(payload.solvingSteps());
        String variants = writeList(payload.variants());
        String followUps = writeList(payload.followUpSuggestions());

        Integer analysisCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM mistake_analysis WHERE mistake_id = ?",
            Integer.class,
            mistakeId
        );

        if (analysisCount != null && analysisCount > 0) {
            jdbcTemplate.update(
                "UPDATE mistake_analysis SET knowledge_points_json = ?, error_type = ?, solving_steps_json = ?, variants_json = ?, follow_up_json = ?, updated_at = ? WHERE mistake_id = ?",
                kp,
                payload.errorType(),
                steps,
                variants,
                followUps,
                Timestamp.valueOf(LocalDateTime.now()),
                mistakeId
            );
        } else {
            jdbcTemplate.update(
                "INSERT INTO mistake_analysis(mistake_id, knowledge_points_json, error_type, solving_steps_json, variants_json, follow_up_json, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?)",
                mistakeId,
                kp,
                payload.errorType(),
                steps,
                variants,
                followUps,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
            );
        }

        jdbcTemplate.update("UPDATE mistake_records SET updated_at = ? WHERE id = ?", Timestamp.valueOf(LocalDateTime.now()), mistakeId);

        Integer taskCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM review_tasks WHERE user_id = ? AND mistake_id = ? AND completed = 0",
            Integer.class,
            userId,
            mistakeId
        );
        if (taskCount == null || taskCount == 0) {
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(
                "INSERT INTO review_tasks(user_id, mistake_id, due_date, completed, repetition, interval_days, ease_factor, suspended, last_grade, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                userId,
                mistakeId,
                Timestamp.valueOf(now.plusDays(1)),
                0,
                1,
                1,
                2.5d,
                0,
                null,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
            );
        }

        return new MistakeAnalysisView(
            payload.knowledgePoints(),
            payload.errorType(),
            payload.solvingSteps(),
            payload.variants(),
            payload.followUpSuggestions()
        );
    }

    private Long extractId(KeyHolder keyHolder, String errorMessage) {
        try {
            Number key = keyHolder.getKey();
            if (key != null) {
                return key.longValue();
            }
        } catch (Exception ignored) {
        }
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            Object idValue = keys.get("id");
            if (idValue instanceof Number number) {
                return number.longValue();
            }
            Object firstValue = keys.values().stream().findFirst().orElse(null);
            if (firstValue instanceof Number number) {
                return number.longValue();
            }
        }
        throw new AppException(500, errorMessage);
    }

    private String writeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception ex) {
            throw new AppException(500, "Serialize analysis result failed");
        }
    }

    private List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private record MistakeRow(Long id, String title, String content) {
    }
}
