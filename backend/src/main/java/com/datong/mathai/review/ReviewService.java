package com.datong.mathai.review;

import com.datong.mathai.common.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReviewService {

    private static final int BOX_ONE = 1;
    private static final int BOX_TWO = 2;
    private static final int BOX_THREE = 3;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ReviewService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ReviewTaskView> list(Long userId, String scope) {
        if ("due".equalsIgnoreCase(scope)) {
            return loadDueTaskRows(userId).stream().map(this::toView).toList();
        }
        return jdbcTemplate.query(
            baseSql() + " ORDER BY r.suspended ASC, r.completed ASC, r.due_date ASC, r.id ASC",
            (rs, rowNum) -> toView(mapRow(rs)),
            userId
        );
    }

    public ReviewTaskView next(Long userId) {
        List<ReviewTaskRow> dueRows = loadDueTaskRows(userId);
        if (!dueRows.isEmpty()) {
            return toView(dueRows.get(0));
        }

        List<ReviewTaskView> upcomingRows = jdbcTemplate.query(
            baseSql() + " AND r.completed = 0 AND r.suspended = 0 ORDER BY r.due_date ASC, r.id ASC LIMIT 1",
            (rs, rowNum) -> toView(mapRow(rs)),
            userId
        );
        return upcomingRows.isEmpty() ? null : upcomingRows.get(0);
    }

    public ReviewTaskView complete(Long userId, Long taskId) {
        return applyGrade(userId, taskId, "easy");
    }

    public ReviewTaskView rate(Long userId, Long taskId, String gradeRaw) {
        return applyGrade(userId, taskId, gradeRaw);
    }

    public ReviewSubmitResponse submit(Long userId, ReviewSubmitRequest request) {
        if (request.answers() == null) {
            throw new AppException(400, "answers is required");
        }

        List<ReviewTaskRow> dueRows = loadDueTaskRows(userId);
        LocalDateTime now = LocalDateTime.now();
        Map<Long, String> answersByTaskId = new LinkedHashMap<>();
        for (ReviewSubmitAnswerRequest answer : request.answers()) {
            if (answer == null || answer.taskId() == null) {
                continue;
            }
            answersByTaskId.put(answer.taskId(), answer.answer());
        }

        List<ReviewSubmitItemResult> results = new ArrayList<>();
        int answeredCount = 0;
        int correctCount = 0;

        for (ReviewTaskRow row : dueRows) {
            String rawAnswer = answersByTaskId.get(row.id());
            if (rawAnswer == null || rawAnswer.trim().isBlank()) {
                results.add(new ReviewSubmitItemResult(
                    row.id(),
                    false,
                    false,
                    row.officialAnswer(),
                    row.officialExplanation(),
                    clampBox(row.repetition()),
                    false
                ));
                continue;
            }

            answeredCount++;
            boolean correct = isAnswerCorrect(normalizeUserAnswer(rawAnswer, row.type()), row.answerJson());
            if (correct) {
                correctCount++;
            }

            TransitionResult transition = transitionTask(userId, row, correct, correct ? "CORRECT" : "AGAIN", now, true);
            results.add(new ReviewSubmitItemResult(
                row.id(),
                true,
                correct,
                row.officialAnswer(),
                row.officialExplanation(),
                transition.nextBox(),
                transition.removedFromMistakes()
            ));
        }

        return new ReviewSubmitResponse(now, dueRows.size(), answeredCount, correctCount, results);
    }

    private ReviewTaskView applyGrade(Long userId, Long taskId, String gradeRaw) {
        if (gradeRaw == null || gradeRaw.isBlank()) {
            throw new AppException(400, "grade is required");
        }
        String grade = gradeRaw.trim().toLowerCase(Locale.ROOT);
        if (!grade.equals("again") && !grade.equals("hard") && !grade.equals("easy")) {
            throw new AppException(400, "grade must be again|hard|easy");
        }

        ReviewTaskRow row = getTaskRowById(userId, taskId);
        LocalDateTime now = LocalDateTime.now();
        boolean correct = !grade.equals("again");
        TransitionResult transition = transitionTask(userId, row, correct, grade.toUpperCase(Locale.ROOT), now, true);

        if (transition.removedFromMistakes()) {
            return new ReviewTaskView(
                row.id(),
                row.mistakeId(),
                row.questionId(),
                row.chapterId(),
                row.difficulty(),
                row.questionTitle(),
                row.questionContent(),
                row.type(),
                row.options(),
                row.sourceLabel(),
                row.sourceSnapshotPath(),
                row.explanationSource(),
                row.explanationReviewStatus(),
                row.officialAnswer(),
                row.officialExplanation(),
                now,
                true,
                true,
                BOX_THREE,
                7,
                row.easeFactor(),
                grade.toUpperCase(Locale.ROOT),
                now
            );
        }

        return getTaskById(userId, taskId);
    }

    private TransitionResult transitionTask(
        Long userId,
        ReviewTaskRow row,
        boolean correct,
        String gradeLabel,
        LocalDateTime now,
        boolean removeWhenCompleted
    ) {
        int currentBox = clampBox(row.repetition());
        double nextEaseFactor = row.easeFactor();

        if (!correct) {
            jdbcTemplate.update(
                """
                    UPDATE review_tasks
                    SET due_date = ?, completed = 0, suspended = 0, repetition = 1, interval_days = 1, ease_factor = ?, last_grade = ?, completed_at = NULL, updated_at = ?
                    WHERE id = ? AND user_id = ?
                    """,
                Timestamp.valueOf(now.plusDays(1)),
                nextEaseFactor,
                gradeLabel,
                Timestamp.valueOf(now),
                row.id(),
                userId
            );
            jdbcTemplate.update(
                "UPDATE mistake_records SET status = 'REVIEWING', updated_at = ? WHERE id = ? AND user_id = ?",
                Timestamp.valueOf(now),
                row.mistakeId(),
                userId
            );
            return new TransitionResult(BOX_ONE, false);
        }

        if (currentBox == BOX_THREE && removeWhenCompleted) {
            jdbcTemplate.update("DELETE FROM mistake_records WHERE id = ? AND user_id = ?", row.mistakeId(), userId);
            return new TransitionResult(0, true);
        }

        int nextBox = currentBox == BOX_ONE ? BOX_TWO : BOX_THREE;
        int intervalDays = nextBox == BOX_TWO ? 3 : 7;
        jdbcTemplate.update(
            """
                UPDATE review_tasks
                SET due_date = ?, completed = 0, suspended = 0, repetition = ?, interval_days = ?, ease_factor = ?, last_grade = ?, completed_at = NULL, updated_at = ?
                WHERE id = ? AND user_id = ?
                """,
            Timestamp.valueOf(now.plusDays(intervalDays)),
            nextBox,
            intervalDays,
            nextEaseFactor,
            gradeLabel,
            Timestamp.valueOf(now),
            row.id(),
            userId
        );
        jdbcTemplate.update(
            "UPDATE mistake_records SET status = 'REVIEWING', updated_at = ? WHERE id = ? AND user_id = ?",
            Timestamp.valueOf(now),
            row.mistakeId(),
            userId
        );
        return new TransitionResult(nextBox, false);
    }

    private List<ReviewTaskRow> loadDueTaskRows(Long userId) {
        return jdbcTemplate.query(
            dueSql(),
            (rs, rowNum) -> mapRow(rs),
            userId,
            Timestamp.valueOf(LocalDateTime.now())
        );
    }

    private ReviewTaskRow getTaskRowById(Long userId, Long taskId) {
        List<ReviewTaskRow> rows = jdbcTemplate.query(
            baseSql() + " AND r.id = ?",
            (rs, rowNum) -> mapRow(rs),
            userId,
            taskId
        );
        if (rows.isEmpty()) {
            throw new AppException(404, "Review task not found");
        }
        return rows.get(0);
    }

    private ReviewTaskView getTaskById(Long userId, Long taskId) {
        return toView(getTaskRowById(userId, taskId));
    }

    private String baseSql() {
        return """
            SELECT r.id, r.mistake_id, r.due_date, r.completed, r.suspended, r.repetition, r.interval_days, r.ease_factor, r.last_grade, r.completed_at,
                   m.question_id, m.chapter_id, m.difficulty, m.question_title, m.question_content,
                   q.type, q.options_json, q.answer_json, q.explanation,
                   q.source_label, q.source_snapshot_path, q.explanation_source, q.explanation_review_status
            FROM review_tasks r
            JOIN mistake_records m ON r.mistake_id = m.id
            LEFT JOIN questions q ON q.id = m.question_id
            WHERE r.user_id = ?
            """;
    }

    private String dueSql() {
        return baseSql() + """
             AND r.completed = 0
             AND r.suspended = 0
             AND r.due_date <= ?
             AND m.question_id IS NOT NULL
             AND q.id IS NOT NULL
             AND q.import_status = 'READY'
             ORDER BY r.due_date ASC, r.id ASC
            """;
    }

    private ReviewTaskRow mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ReviewTaskRow(
            rs.getLong("id"),
            rs.getLong("mistake_id"),
            rs.getObject("question_id") == null ? null : rs.getLong("question_id"),
            rs.getObject("chapter_id") == null ? null : rs.getLong("chapter_id"),
            rs.getString("difficulty"),
            rs.getString("question_title"),
            rs.getString("question_content"),
            rs.getString("type"),
            parseList(rs.getString("options_json")),
            rs.getString("answer_json"),
            parseAnswerForDisplay(rs.getString("answer_json")),
            rs.getString("explanation"),
            rs.getString("source_label"),
            rs.getString("source_snapshot_path"),
            rs.getString("explanation_source"),
            rs.getString("explanation_review_status"),
            rs.getTimestamp("due_date").toLocalDateTime(),
            rs.getBoolean("completed"),
            rs.getBoolean("suspended"),
            clampBox(rs.getInt("repetition")),
            rs.getInt("interval_days"),
            rs.getDouble("ease_factor"),
            rs.getString("last_grade"),
            rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime()
        );
    }

    private ReviewTaskView toView(ReviewTaskRow row) {
        return new ReviewTaskView(
            row.id(),
            row.mistakeId(),
            row.questionId(),
            row.chapterId(),
            row.difficulty(),
            row.questionTitle(),
            row.questionContent(),
            row.type(),
            row.options(),
            row.sourceLabel(),
            row.sourceSnapshotPath(),
            row.explanationSource(),
            row.explanationReviewStatus(),
            row.officialAnswer(),
            row.officialExplanation(),
            row.dueDate(),
            row.completed(),
            row.suspended(),
            row.repetition(),
            row.intervalDays(),
            row.easeFactor(),
            row.lastGrade(),
            row.completedAt()
        );
    }

    private int clampBox(int repetition) {
        if (repetition <= BOX_ONE) {
            return BOX_ONE;
        }
        if (repetition >= BOX_THREE) {
            return BOX_THREE;
        }
        return repetition;
    }

    private String normalizeUserAnswer(String answer, String type) {
        String normalized = answer == null ? "" : answer.trim();
        if (normalized.isBlank()) {
            throw new AppException(400, "answer is required");
        }
        String canonical = ("SINGLE".equalsIgnoreCase(type) || "MULTI".equalsIgnoreCase(type))
            ? normalized.toUpperCase(Locale.ROOT)
            : normalized;
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

    private record ReviewTaskRow(
        Long id,
        Long mistakeId,
        Long questionId,
        Long chapterId,
        String difficulty,
        String questionTitle,
        String questionContent,
        String type,
        List<String> options,
        String answerJson,
        String officialAnswer,
        String officialExplanation,
        String sourceLabel,
        String sourceSnapshotPath,
        String explanationSource,
        String explanationReviewStatus,
        LocalDateTime dueDate,
        boolean completed,
        boolean suspended,
        int repetition,
        int intervalDays,
        double easeFactor,
        String lastGrade,
        LocalDateTime completedAt
    ) {
    }

    private record TransitionResult(int nextBox, boolean removedFromMistakes) {
    }
}
