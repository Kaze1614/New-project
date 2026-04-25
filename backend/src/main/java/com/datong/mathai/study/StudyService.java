package com.datong.mathai.study;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class StudyService {

    private static final int SESSION_QUESTION_COUNT = 20;
    private static final int DEFAULT_DURATION_SECONDS = 30 * 60;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public StudyService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public StudySessionDetail createSession(Long userId, CreateStudySessionRequest request) {
        Long chapterId = request.chapterId();
        int durationSeconds = request.durationSeconds() == null ? DEFAULT_DURATION_SECONDS : request.durationSeconds();
        LocalDateTime now = LocalDateTime.now();

        List<QuestionSeed> selected = selectQuestions(chapterId, SESSION_QUESTION_COUNT);
        if (selected.size() < SESSION_QUESTION_COUNT) {
            throw new AppException(400, "题库可用题量不足20道，请先补充例题");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO study_sessions(user_id, chapter_id, duration_seconds, started_at, created_at, updated_at) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            if (chapterId == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, chapterId);
            }
            ps.setInt(3, durationSeconds);
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setTimestamp(6, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long sessionId = extractId(keyHolder, "Create study session failed");
        for (int i = 0; i < selected.size(); i++) {
            QuestionSeed seed = selected.get(i);
            jdbcTemplate.update(
                "INSERT INTO study_session_items(session_id, question_id, sort_order, created_at, updated_at) VALUES(?,?,?,?,?)",
                sessionId,
                seed.id(),
                i + 1,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
            );
        }
        return buildSessionDetail(userId, sessionId);
    }

    public StudySessionDetail getSession(Long userId, Long sessionId) {
        return buildSessionDetail(userId, sessionId);
    }

    public StudySessionDetail saveAnswer(Long userId, Long sessionId, SaveStudyAnswerRequest request) {
        SessionMeta sessionMeta = requireSessionOwner(userId, sessionId);
        if (sessionMeta.submittedAt() != null) {
            throw new AppException(400, "当前会话已提交，不能继续作答");
        }

        ItemMeta itemMeta = findItemMeta(sessionId, request.itemId());
        String normalizedJson = normalizeUserAnswer(request.answer(), itemMeta.type());
        LocalDateTime now = LocalDateTime.now();

        int affected = jdbcTemplate.update(
            """
                UPDATE study_session_items
                SET user_answer_json = ?, is_correct = NULL, answered_at = ?, updated_at = ?
                WHERE id = ? AND session_id = ?
                """,
            normalizedJson,
            Timestamp.valueOf(now),
            Timestamp.valueOf(now),
            request.itemId(),
            sessionId
        );
        if (affected == 0) {
            throw new AppException(404, "Study item not found");
        }
        jdbcTemplate.update("UPDATE study_sessions SET updated_at = ? WHERE id = ?", Timestamp.valueOf(now), sessionId);
        return buildSessionDetail(userId, sessionId);
    }

    public StudySessionDetail submit(Long userId, Long sessionId) {
        SessionMeta sessionMeta = requireSessionOwner(userId, sessionId);
        if (sessionMeta.submittedAt() != null) {
            return buildSessionDetail(userId, sessionId);
        }

        List<SubmissionRow> rows = jdbcTemplate.query(
            """
                SELECT si.id, si.question_id, si.user_answer_json,
                       q.chapter_id, q.title, q.content, q.type, q.answer_json, q.explanation, q.difficulty
                FROM study_session_items si
                JOIN questions q ON q.id = si.question_id
                WHERE si.session_id = ?
                ORDER BY si.sort_order ASC
                """,
            (rs, rowNum) -> new SubmissionRow(
                rs.getLong("id"),
                rs.getLong("question_id"),
                rs.getString("user_answer_json"),
                rs.getLong("chapter_id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("type"),
                rs.getString("answer_json"),
                rs.getString("explanation"),
                rs.getString("difficulty")
            ),
            sessionId
        );
        if (rows.isEmpty()) {
            throw new AppException(400, "当前会话无题目数据");
        }

        LocalDateTime now = LocalDateTime.now();
        for (SubmissionRow row : rows) {
            boolean correct = isAnswerCorrect(row.userAnswerJson(), row.answerJson());
            jdbcTemplate.update(
                "UPDATE study_session_items SET is_correct = ?, updated_at = ? WHERE id = ?",
                correct ? 1 : 0,
                Timestamp.valueOf(now),
                row.itemId()
            );

            if (!correct && row.userAnswerJson() != null && !row.userAnswerJson().isBlank()) {
                long mistakeId = insertMistake(userId, row, now);
                ensureReviewTask(userId, mistakeId, row.questionId(), now);
            }
        }

        jdbcTemplate.update(
            "UPDATE study_sessions SET submitted_at = ?, updated_at = ? WHERE id = ? AND user_id = ?",
            Timestamp.valueOf(now),
            Timestamp.valueOf(now),
            sessionId,
            userId
        );
        return buildSessionDetail(userId, sessionId);
    }

    private StudySessionDetail buildSessionDetail(Long userId, Long sessionId) {
        SessionMeta sessionMeta = requireSessionOwner(userId, sessionId);
        boolean reveal = sessionMeta.submittedAt() != null;

        List<StudySessionItemView> items = jdbcTemplate.query(
            """
                SELECT si.id AS item_id, si.question_id, si.sort_order, si.user_answer_json, si.is_correct, si.answered_at,
                       q.type, q.difficulty, q.title, q.content, q.options_json, q.answer_json, q.explanation,
                       q.source_label, q.source_snapshot_path, q.explanation_source, q.explanation_review_status
                FROM study_session_items si
                JOIN questions q ON q.id = si.question_id
                WHERE si.session_id = ?
                ORDER BY si.sort_order ASC
                """,
            (rs, rowNum) -> {
                String userAnswerJson = rs.getString("user_answer_json");
                return new StudySessionItemView(
                    rs.getLong("item_id"),
                    rs.getLong("question_id"),
                    rs.getInt("sort_order"),
                    rs.getString("type"),
                    rs.getString("difficulty"),
                    rs.getString("title"),
                    rs.getString("content"),
                    parseList(rs.getString("options_json")),
                    parseAnswerForDisplay(userAnswerJson),
                    userAnswerJson != null && !userAnswerJson.isBlank(),
                    rs.getObject("is_correct") == null ? null : rs.getBoolean("is_correct"),
                    rs.getString("source_label"),
                    rs.getString("source_snapshot_path"),
                    rs.getString("explanation_source"),
                    rs.getString("explanation_review_status"),
                    reveal ? parseAnswerForDisplay(rs.getString("answer_json")) : null,
                    reveal ? rs.getString("explanation") : null,
                    rs.getTimestamp("answered_at") == null ? null : rs.getTimestamp("answered_at").toLocalDateTime()
                );
            },
            sessionId
        );

        int answeredCount = 0;
        int correctCount = 0;
        for (StudySessionItemView item : items) {
            if (item.answered()) {
                answeredCount++;
            }
            if (Boolean.TRUE.equals(item.correct())) {
                correctCount++;
            }
        }

        return new StudySessionDetail(
            sessionMeta.id(),
            sessionMeta.chapterId(),
            sessionMeta.durationSeconds(),
            sessionMeta.startedAt(),
            sessionMeta.submittedAt(),
            reveal,
            items.size(),
            answeredCount,
            correctCount,
            items
        );
    }

    private List<QuestionSeed> selectQuestions(Long chapterId, int limit) {
        List<QuestionSeed> selected;
        if (chapterId == null) {
            selected = jdbcTemplate.query(
                "SELECT id, chapter_id FROM questions WHERE import_status = 'READY' ORDER BY RAND() LIMIT ?",
                (rs, rowNum) -> new QuestionSeed(rs.getLong("id"), rs.getLong("chapter_id")),
                limit
            );
            return selected;
        }

        selected = jdbcTemplate.query(
            "SELECT id, chapter_id FROM questions WHERE chapter_id = ? AND import_status = 'READY' ORDER BY RAND() LIMIT ?",
            (rs, rowNum) -> new QuestionSeed(rs.getLong("id"), rs.getLong("chapter_id")),
            chapterId,
            limit
        );
        if (selected.size() >= limit) {
            return selected;
        }

        Set<Long> usedIds = new LinkedHashSet<>();
        for (QuestionSeed questionSeed : selected) {
            usedIds.add(questionSeed.id());
        }
        int remain = limit - selected.size();
        StringBuilder sql = new StringBuilder("SELECT id, chapter_id FROM questions WHERE import_status = 'READY'");
        List<Object> args = new ArrayList<>();
        if (!usedIds.isEmpty()) {
            sql.append(" AND id NOT IN (");
            for (int i = 0; i < usedIds.size(); i++) {
                if (i > 0) {
                    sql.append(",");
                }
                sql.append("?");
            }
            sql.append(")");
            args.addAll(usedIds);
        }
        sql.append(" ORDER BY RAND() LIMIT ?");
        args.add(remain);

        List<QuestionSeed> filler = jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> new QuestionSeed(rs.getLong("id"), rs.getLong("chapter_id")),
            args.toArray()
        );
        selected = new ArrayList<>(selected);
        selected.addAll(filler);
        return selected;
    }

    private long insertMistake(Long userId, SubmissionRow row, LocalDateTime now) {
        Long existingId = findExistingMistakeId(userId, row.questionId());
        if (existingId != null) {
            jdbcTemplate.update(
                """
                    UPDATE mistake_records
                    SET chapter_id = ?, difficulty = ?, question_title = ?, question_content = ?, image_url = ?, status = ?, updated_at = ?
                    WHERE id = ? AND user_id = ?
                    """,
                row.chapterId(),
                normalizeDifficulty(row.difficulty()),
                row.title(),
                row.content(),
                null,
                "REVIEWING",
                Timestamp.valueOf(now),
                existingId,
                userId
            );
            return existingId;
        }

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
            ps.setLong(3, row.chapterId());
            ps.setString(4, normalizeDifficulty(row.difficulty()));
            ps.setString(5, row.title());
            ps.setString(6, row.content());
            ps.setObject(7, null);
            ps.setString(8, "REVIEWING");
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.setTimestamp(10, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);
        return extractId(keyHolder, "Create mistake failed");
    }

    private Long findExistingMistakeId(Long userId, Long questionId) {
        if (questionId == null) {
            return null;
        }
        List<Long> rows = jdbcTemplate.query(
            """
                SELECT id
                FROM mistake_records
                WHERE user_id = ? AND question_id = ?
                ORDER BY updated_at DESC, id DESC
                LIMIT 1
                """,
            (rs, rowNum) -> rs.getLong("id"),
            userId,
            questionId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void ensureReviewTask(Long userId, long mistakeId, long questionId, LocalDateTime now) {
        Integer activeCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(1)
                FROM review_tasks r
                JOIN mistake_records m ON m.id = r.mistake_id
                WHERE r.user_id = ? AND m.question_id = ? AND r.completed = 0 AND r.suspended = 0
                """,
            Integer.class,
            userId,
            questionId
        );
        if (activeCount != null && activeCount > 0) {
            return;
        }
        jdbcTemplate.update(
            """
                INSERT INTO review_tasks(user_id, mistake_id, due_date, completed, repetition, interval_days, ease_factor, suspended, last_grade, created_at, updated_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)
                """,
            userId,
            mistakeId,
            Timestamp.valueOf(now),
            0,
            1,
            0,
            2.5d,
            0,
            null,
            Timestamp.valueOf(now),
            Timestamp.valueOf(now)
        );
    }

    private SessionMeta requireSessionOwner(Long userId, Long sessionId) {
        List<SessionMeta> rows = jdbcTemplate.query(
            """
                SELECT id, chapter_id, duration_seconds, started_at, submitted_at
                FROM study_sessions
                WHERE id = ? AND user_id = ?
                """,
            (rs, rowNum) -> new SessionMeta(
                rs.getLong("id"),
                rs.getObject("chapter_id") == null ? null : rs.getLong("chapter_id"),
                rs.getInt("duration_seconds"),
                rs.getTimestamp("started_at").toLocalDateTime(),
                rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toLocalDateTime()
            ),
            sessionId,
            userId
        );
        if (rows.isEmpty()) {
            throw new AppException(404, "Study session not found");
        }
        return rows.get(0);
    }

    private ItemMeta findItemMeta(Long sessionId, Long itemId) {
        List<ItemMeta> rows = jdbcTemplate.query(
            """
                SELECT si.id, q.type
                FROM study_session_items si
                JOIN questions q ON q.id = si.question_id
                WHERE si.id = ? AND si.session_id = ?
                """,
            (rs, rowNum) -> new ItemMeta(rs.getLong("id"), rs.getString("type")),
            itemId,
            sessionId
        );
        if (rows.isEmpty()) {
            throw new AppException(404, "Study item not found");
        }
        return rows.get(0);
    }

    private String normalizeUserAnswer(String answer, String type) {
        String normalized = answer.trim();
        if (normalized.isBlank()) {
            throw new AppException(400, "answer is required");
        }
        String canonical;
        if ("SINGLE".equalsIgnoreCase(type) || "MULTI".equalsIgnoreCase(type)) {
            canonical = normalized.toUpperCase(Locale.ROOT);
        } else {
            canonical = normalized;
        }
        return writeList(List.of(canonical));
    }

    private boolean isAnswerCorrect(String userAnswerJson, String officialAnswerJson) {
        if (userAnswerJson == null || userAnswerJson.isBlank()) {
            return false;
        }
        List<String> user = canonicalizeList(parseList(userAnswerJson));
        List<String> official = canonicalizeList(parseList(officialAnswerJson));
        return user.equals(official);
    }

    private List<String> canonicalizeList(List<String> source) {
        List<String> canonical = new ArrayList<>();
        for (String value : source) {
            if (value == null) {
                continue;
            }
            canonical.add(value.trim().toUpperCase(Locale.ROOT));
        }
        Collections.sort(canonical);
        return canonical;
    }

    private String parseAnswerForDisplay(String rawJson) {
        List<String> values = parseList(rawJson);
        if (values.isEmpty()) {
            return null;
        }
        return String.join(", ", values);
    }

    private List<String> parseList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            throw new AppException(500, "Serialize answer failed");
        }
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return "MEDIUM";
        }
        return difficulty.trim().toUpperCase(Locale.ROOT);
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

    private record QuestionSeed(Long id, Long chapterId) {
    }

    private record SessionMeta(Long id, Long chapterId, int durationSeconds, LocalDateTime startedAt, LocalDateTime submittedAt) {
    }

    private record ItemMeta(Long id, String type) {
    }

    private record SubmissionRow(
        Long itemId,
        Long questionId,
        String userAnswerJson,
        Long chapterId,
        String title,
        String content,
        String type,
        String answerJson,
        String explanation,
        String difficulty
    ) {
    }
}
