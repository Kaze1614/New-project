package com.datong.mathai.admin;

import com.datong.mathai.common.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminMathQuestionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private final RowMapper<MathQuestionDetail> detailMapper = (rs, rowNum) -> new MathQuestionDetail(
        rs.getLong("id"),
        rs.getString("image_url"),
        rs.getString("raw_text_latex"),
        rs.getString("answer_latex"),
        rs.getString("teacher_explanation"),
        rs.getString("book_name"),
        rs.getString("chapter_name"),
        rs.getString("section_name"),
        getNullableInteger(rs.getObject("source_year")),
        rs.getString("source_paper"),
        rs.getInt("question_no"),
        buildSourceLabel(getNullableInteger(rs.getObject("source_year")), rs.getString("source_paper"), rs.getInt("question_no")),
        rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("updated_at").toLocalDateTime()
    );

    public AdminMathQuestionService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public PageResult<MathQuestionListItem> list(String keyword, Integer page, Integer size) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.min(Math.max(size == null ? 20 : size, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        ArrayList<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (raw_text_latex LIKE ? OR answer_latex LIKE ? OR teacher_explanation LIKE ? OR book_name LIKE ? OR chapter_name LIKE ? OR section_name LIKE ? OR source_paper LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            for (int i = 0; i < 7; i++) {
                args.add(like);
            }
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM math_questions" + where, Long.class, args.toArray());

        ArrayList<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(safeSize);
        queryArgs.add(offset);
        var items = jdbcTemplate.query(
            """
                SELECT id, question_no, book_name, chapter_name, section_name, raw_text_latex,
                       source_year, source_paper, updated_at
                FROM math_questions
                """ + where + " ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
            (rs, rowNum) -> {
                Integer sourceYear = getNullableInteger(rs.getObject("source_year"));
                String sourcePaper = rs.getString("source_paper");
                int questionNo = rs.getInt("question_no");
                return new MathQuestionListItem(
                    rs.getLong("id"),
                    questionNo,
                    rs.getString("book_name"),
                    rs.getString("chapter_name"),
                    rs.getString("section_name"),
                    preview(rs.getString("raw_text_latex")),
                    sourceYear,
                    sourcePaper,
                    buildSourceLabel(sourceYear, sourcePaper, questionNo),
                    rs.getTimestamp("updated_at").toLocalDateTime()
                );
            },
            queryArgs.toArray()
        );

        return new PageResult<>(items, safePage, safeSize, total == null ? 0 : total);
    }

    public MathQuestionDetail get(Long id) {
        var rows = jdbcTemplate.query(
            """
                SELECT id, image_url, raw_text_latex, answer_latex, teacher_explanation,
                       book_name, chapter_name, section_name, source_year, source_paper,
                       question_no, created_at, updated_at
                FROM math_questions
                WHERE id = ?
                """,
            detailMapper,
            id
        );
        if (rows.isEmpty()) {
            throw new AppException(404, "Question not found");
        }
        return rows.get(0);
    }

    public MathQuestionDetail create(MathQuestionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO math_questions(
                        image_url, raw_text_latex, answer_latex, teacher_explanation,
                        book_name, chapter_name, section_name, source_year, source_paper,
                        question_no, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                Statement.RETURN_GENERATED_KEYS
            );
            bindRequest(ps, request);
            ps.setTimestamp(11, Timestamp.valueOf(now));
            ps.setTimestamp(12, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long id = extractId(keyHolder, "Create math question failed");
        syncRuntimeQuestion(id, request, now);
        return get(id);
    }

    public MathQuestionDetail update(Long id, MathQuestionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        int affected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    UPDATE math_questions
                    SET image_url = ?, raw_text_latex = ?, answer_latex = ?, teacher_explanation = ?,
                        book_name = ?, chapter_name = ?, section_name = ?, source_year = ?, source_paper = ?,
                        question_no = ?, updated_at = ?
                    WHERE id = ?
                    """
            );
            bindRequest(ps, request);
            ps.setTimestamp(11, Timestamp.valueOf(now));
            ps.setLong(12, id);
            return ps;
        });
        if (affected == 0) {
            throw new AppException(404, "Question not found");
        }
        syncRuntimeQuestion(id, request, now);
        return get(id);
    }

    public void delete(Long id) {
        int affected = jdbcTemplate.update("DELETE FROM math_questions WHERE id = ?", id);
        if (affected == 0) {
            throw new AppException(404, "Question not found");
        }
        jdbcTemplate.update("UPDATE questions SET import_status = 'REMOVED' WHERE source_math_question_id = ?", id);
    }

    private void syncRuntimeQuestion(Long mathQuestionId, MathQuestionRequest request, LocalDateTime now) {
        Long sectionId = resolveSectionId(request);
        String sourceLabel = buildSourceLabel(request.sourceYear(), request.sourcePaper(), request.questionNo());
        String strippedContent = stripSourcePrefix(request.rawTextLatex().trim());
        String title = preview(strippedContent);
        String content = sourceLabel.isBlank() ? strippedContent : (sourceLabel + " " + strippedContent).trim();
        String explanation = blankToNull(request.teacherExplanation());
        String answerJson = serializeAnswerList(normalizeAnswers(request.answerLatex()));
        String explanationSource = explanation == null ? "NONE" : "TEACHER_GENERATED";

        List<Long> runtimeRows = jdbcTemplate.query(
            "SELECT id FROM questions WHERE source_math_question_id = ? LIMIT 1",
            (rs, rowNum) -> rs.getLong("id"),
            mathQuestionId
        );

        if (runtimeRows.isEmpty()) {
            jdbcTemplate.update(
                """
                    INSERT INTO questions(
                        source_math_question_id, chapter_id, title, content, type, options_json, answer_json, explanation, difficulty,
                        source_year, source_paper, source_question_no, source_label, source_snapshot_path, exam_section,
                        import_batch, import_status, explanation_source, explanation_review_status, created_at
                    ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """,
                mathQuestionId,
                sectionId,
                title,
                content,
                "FILL",
                null,
                answerJson,
                explanation,
                "MEDIUM",
                request.sourceYear(),
                blankToNull(request.sourcePaper()),
                String.valueOf(request.questionNo()),
                sourceLabel,
                blankToNull(request.imageUrl()),
                request.sectionName().trim(),
                "ADMIN_SYNC",
                "READY",
                explanationSource,
                "PENDING_REVIEW",
                Timestamp.valueOf(now)
            );
            return;
        }

        jdbcTemplate.update(
            """
                UPDATE questions
                SET chapter_id = ?, title = ?, content = ?, type = ?, options_json = ?, answer_json = ?, explanation = ?, difficulty = ?,
                    source_year = ?, source_paper = ?, source_question_no = ?, source_label = ?, source_snapshot_path = ?, exam_section = ?,
                    import_batch = ?, import_status = ?, explanation_source = ?, explanation_review_status = ?
                WHERE source_math_question_id = ?
                """,
            sectionId,
            title,
            content,
            "FILL",
            null,
            answerJson,
            explanation,
            "MEDIUM",
            request.sourceYear(),
            blankToNull(request.sourcePaper()),
            String.valueOf(request.questionNo()),
            sourceLabel,
            blankToNull(request.imageUrl()),
            request.sectionName().trim(),
            "ADMIN_SYNC",
            "READY",
            explanationSource,
            "PENDING_REVIEW",
            mathQuestionId
        );
    }

    private Long resolveSectionId(MathQuestionRequest request) {
        List<Long> rows = jdbcTemplate.query(
            """
                SELECT s.id
                FROM chapters s
                JOIN chapters c ON s.parent_id = c.id
                JOIN chapters b ON c.parent_id = b.id
                WHERE b.title = ? AND c.title = ? AND s.title = ?
                ORDER BY s.id ASC
                LIMIT 1
                """,
            (rs, rowNum) -> rs.getLong("id"),
            request.bookName().trim(),
            request.chapterName().trim(),
            request.sectionName().trim()
        );
        if (rows.isEmpty()) {
            throw new AppException(400, "Chapter tree mapping not found");
        }
        return rows.get(0);
    }

    private void bindRequest(PreparedStatement ps, MathQuestionRequest request) throws java.sql.SQLException {
        ps.setString(1, normalizeImageUrl(request.imageUrl()));
        ps.setString(2, request.rawTextLatex().trim());
        ps.setString(3, blankToNull(request.answerLatex()));
        ps.setString(4, blankToNull(request.teacherExplanation()));
        ps.setString(5, request.bookName().trim());
        ps.setString(6, request.chapterName().trim());
        ps.setString(7, request.sectionName().trim());
        if (request.sourceYear() == null) {
            ps.setObject(8, null);
        } else {
            ps.setInt(8, request.sourceYear());
        }
        ps.setString(9, blankToNull(request.sourcePaper()));
        ps.setInt(10, request.questionNo());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private List<String> normalizeAnswers(String answerLatex) {
        String normalized = blankToNull(answerLatex);
        if (normalized == null) {
            return List.of();
        }
        if (normalized.matches("^[A-Za-z]{2,}$")) {
            List<String> result = new ArrayList<>();
            for (char item : normalized.toUpperCase().toCharArray()) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        if (normalized.matches("^[A-Za-z](?:[\\s,;/|、，]+[A-Za-z])+$")) {
            List<String> result = new ArrayList<>();
            for (String item : normalized.split("[\\s,;/|、，]+")) {
                if (!item.isBlank()) {
                    result.add(item.trim().toUpperCase());
                }
            }
            return result;
        }
        return List.of(normalized);
    }

    private String serializeAnswerList(List<String> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (Exception ex) {
            throw new AppException(500, "Serialize answer failed");
        }
    }

    private static Integer getNullableInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private static String buildSourceLabel(Integer sourceYear, String sourcePaper, int questionNo) {
        if (sourceYear == null || sourcePaper == null || sourcePaper.isBlank()) {
            return questionNo + ".";
        }
        return questionNo + ".(" + sourceYear + ")(" + sourcePaper + ")";
    }

    private static String preview(String value) {
        if (value == null || value.isBlank()) {
            return "Untitled question";
        }
        String text = value.replaceAll("\\s+", " ").trim();
        return text.length() > 96 ? text.substring(0, 96) + "..." : text;
    }

    private String stripSourcePrefix(String value) {
        return value
            .replaceFirst("^\\s*\\d+\\.\\(\\d{4}\\)\\([^)]*\\)\\s*", "")
            .replaceFirst("^\\s*\\d+\\.\\s*", "")
            .trim();
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
}
