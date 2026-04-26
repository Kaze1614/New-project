package com.datong.mathai.favorite;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public FavoriteService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<FavoriteItem> list(Long userId, Long chapterId, String difficulty, String keyword) {
        StringBuilder sql = new StringBuilder("""
            SELECT f.id, f.question_id, f.chapter_id, f.difficulty, f.title, f.content, f.created_at,
                   q.type AS question_type, q.options_json, q.answer_json, q.explanation, q.source_label
            FROM favorites f
            LEFT JOIN questions q ON f.question_id = q.id
            WHERE f.user_id = ?
            """);
        List<Object> args = new ArrayList<>();
        args.add(userId);

        if (chapterId != null) {
            Set<Long> chapterIds = loadDescendantIds(chapterId);
            if (chapterIds.isEmpty()) {
                chapterIds = Set.of(chapterId);
            }
            sql.append(" AND f.chapter_id IN (")
                .append(chapterIds.stream().map(id -> "?").collect(Collectors.joining(",")))
                .append(")");
            args.addAll(chapterIds);
        }
        if (difficulty != null && !difficulty.isBlank()) {
            sql.append(" AND f.difficulty = ?");
            args.add(difficulty.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (f.title LIKE ? OR f.content LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY f.created_at DESC");

        return jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> mapFavoriteItem(rs),
            args.toArray()
        );
    }

    public FavoriteItem create(Long userId, CreateFavoriteRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (request.questionId() != null) {
            List<FavoriteItem> existing = jdbcTemplate.query(
                """
                    SELECT f.id, f.question_id, f.chapter_id, f.difficulty, f.title, f.content, f.created_at,
                           q.type AS question_type, q.options_json, q.answer_json, q.explanation, q.source_label
                    FROM favorites f
                    LEFT JOIN questions q ON f.question_id = q.id
                    WHERE f.user_id = ? AND f.question_id = ? LIMIT 1
                    """,
                (rs, rowNum) -> mapFavoriteItem(rs),
                userId,
                request.questionId()
            );
            if (!existing.isEmpty()) {
                FavoriteItem item = existing.get(0);
                jdbcTemplate.update(
                    "UPDATE favorites SET chapter_id = ?, difficulty = ?, title = ?, content = ? WHERE id = ? AND user_id = ?",
                    request.chapterId(),
                    request.difficulty() == null ? null : request.difficulty().trim().toUpperCase(),
                    request.title(),
                    request.content(),
                    item.id(),
                    userId
                );
                return new FavoriteItem(
                    item.id(),
                    request.questionId(),
                    request.chapterId(),
                    normalizeDifficulty(request.difficulty()),
                    request.title(),
                    request.content(),
                    item.questionType(),
                    item.options(),
                    item.correctAnswer(),
                    item.explanation(),
                    item.sourceLabel(),
                    item.createdAt()
                );
            }
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO favorites(user_id, question_id, chapter_id, difficulty, title, content, created_at) VALUES(?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            if (request.questionId() == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, request.questionId());
            }
            if (request.chapterId() == null) {
                ps.setObject(3, null);
            } else {
                ps.setLong(3, request.chapterId());
            }
            ps.setString(4, normalizeDifficulty(request.difficulty()));
            ps.setString(5, request.title());
            ps.setString(6, request.content());
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long id = extractId(keyHolder, "Create favorite failed");
        return new FavoriteItem(
            id,
            request.questionId(),
            request.chapterId(),
            normalizeDifficulty(request.difficulty()),
            request.title(),
            request.content(),
            null,
            Collections.emptyList(),
            null,
            null,
            null,
            now
        );
    }

    public void delete(Long userId, Long id) {
        int affected = jdbcTemplate.update("DELETE FROM favorites WHERE id = ? AND user_id = ?", id, userId);
        if (affected == 0) {
            throw new AppException(404, "Favorite not found");
        }
    }

    public void addToReview(Long userId, Long favoriteId) {
        var rows = jdbcTemplate.query(
            """
                SELECT f.id, f.question_id, f.chapter_id, f.difficulty, f.title, f.content
                FROM favorites f
                LEFT JOIN questions q ON f.question_id = q.id
                WHERE f.id = ? AND f.user_id = ? AND f.question_id IS NOT NULL AND q.id IS NOT NULL AND q.import_status = 'READY'
                LIMIT 1
                """,
            (rs, rowNum) -> new FavoriteReviewRow(
                rs.getLong("id"),
                rs.getLong("question_id"),
                rs.getObject("chapter_id") == null ? null : rs.getLong("chapter_id"),
                rs.getString("difficulty"),
                rs.getString("title"),
                rs.getString("content")
            ),
            favoriteId,
            userId
        );

        if (rows.isEmpty()) {
            throw new AppException(400, "Favorite question cannot be added to review");
        }

        FavoriteReviewRow row = rows.get(0);
        LocalDateTime now = LocalDateTime.now();

        List<Long> mistakeIds = jdbcTemplate.query(
            "SELECT id FROM mistake_records WHERE user_id = ? AND question_id = ? ORDER BY id ASC",
            (rs, rowNum) -> rs.getLong("id"),
            userId,
            row.questionId()
        );

        Long mistakeId;
        if (mistakeIds.isEmpty()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    """
                        INSERT INTO mistake_records(user_id, question_id, chapter_id, difficulty, question_title, question_content, image_url, status, created_at, updated_at)
                        VALUES(?,?,?,?,?,?,?,?,?,?)
                        """,
                    Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, userId);
                ps.setLong(2, row.questionId());
                ps.setObject(3, row.chapterId());
                ps.setString(4, normalizeDifficulty(row.difficulty()));
                ps.setString(5, row.title());
                ps.setString(6, row.content());
                ps.setObject(7, null);
                ps.setString(8, "REVIEWING");
                ps.setTimestamp(9, Timestamp.valueOf(now));
                ps.setTimestamp(10, Timestamp.valueOf(now));
                return ps;
            }, keyHolder);
            mistakeId = extractId(keyHolder, "Create mistake from favorite failed");
        } else {
            mistakeId = mistakeIds.get(0);
            jdbcTemplate.update(
                """
                    UPDATE mistake_records
                    SET chapter_id = ?, difficulty = ?, question_title = ?, question_content = ?, status = 'REVIEWING', updated_at = ?
                    WHERE id = ? AND user_id = ?
                    """,
                row.chapterId(),
                normalizeDifficulty(row.difficulty()),
                row.title(),
                row.content(),
                Timestamp.valueOf(now),
                mistakeId,
                userId
            );
        }

        Integer taskCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM review_tasks WHERE user_id = ? AND mistake_id = ?",
            Integer.class,
            userId,
            mistakeId
        );

        if (taskCount != null && taskCount > 0) {
            jdbcTemplate.update(
                """
                    UPDATE review_tasks
                    SET due_date = ?, completed = 0, repetition = 1, interval_days = 0, ease_factor = 2.50, suspended = 0,
                        last_grade = 'FAVORITE', completed_at = NULL, updated_at = ?
                    WHERE user_id = ? AND mistake_id = ?
                    """,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                userId,
                mistakeId
            );
            return;
        }

        jdbcTemplate.update(
            """
                INSERT INTO review_tasks(user_id, mistake_id, due_date, completed, repetition, interval_days, ease_factor, suspended, last_grade, completed_at, created_at, updated_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                """,
            userId,
            mistakeId,
            Timestamp.valueOf(now),
            0,
            1,
            0,
            2.50d,
            0,
            "FAVORITE",
            null,
            Timestamp.valueOf(now),
            Timestamp.valueOf(now)
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

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return null;
        }
        return difficulty.trim().toUpperCase();
    }

    private FavoriteItem mapFavoriteItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new FavoriteItem(
            rs.getLong("id"),
            rs.getObject("question_id") == null ? null : rs.getLong("question_id"),
            rs.getObject("chapter_id") == null ? null : rs.getLong("chapter_id"),
            rs.getString("difficulty"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("question_type"),
            parseList(rs.getString("options_json")),
            stringifyAnswer(rs.getString("answer_json")),
            rs.getString("explanation"),
            rs.getString("source_label"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private Set<Long> loadDescendantIds(Long chapterId) {
        List<ChapterNode> nodes = jdbcTemplate.query(
            "SELECT id, parent_id FROM chapters",
            (rs, rowNum) -> new ChapterNode(
                rs.getLong("id"),
                rs.getObject("parent_id") == null ? null : rs.getLong("parent_id")
            )
        );

        Map<Long, List<Long>> childrenMap = nodes.stream().collect(Collectors.groupingBy(
            node -> node.parentId() == null ? 0L : node.parentId(),
            Collectors.mapping(ChapterNode::id, Collectors.toList())
        ));

        Set<Long> descendants = new LinkedHashSet<>();
        collectDescendants(chapterId, childrenMap, descendants);
        return descendants;
    }

    private void collectDescendants(Long currentId, Map<Long, List<Long>> childrenMap, Set<Long> output) {
        if (currentId == null || output.contains(currentId)) {
            return;
        }
        output.add(currentId);
        for (Long childId : childrenMap.getOrDefault(currentId, Collections.emptyList())) {
            collectDescendants(childId, childrenMap, output);
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

    private String stringifyAnswer(String raw) {
        List<String> answers = parseList(raw);
        if (answers.isEmpty()) {
            return null;
        }
        return answers.stream()
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .map(String::toUpperCase)
            .collect(Collectors.joining(","));
    }

    private record ChapterNode(Long id, Long parentId) {
    }

    private record FavoriteReviewRow(Long id, Long questionId, Long chapterId, String difficulty, String title, String content) {
    }
}
