package com.datong.mathai.admin;

import com.datong.mathai.common.AppException;
import com.datong.mathai.question.QuestionOptionPayload;
import com.datong.mathai.question.QuestionSubQuestionPayload;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AdminMathQuestionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Logger log = LoggerFactory.getLogger(AdminMathQuestionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private final RowMapper<MathQuestionDetail> detailMapper = (rs, rowNum) -> new MathQuestionDetail(
        rs.getLong("id"),
        rs.getString("image_url"),
        rs.getString("question_type"),
        rs.getString("raw_text_latex"),
        parseOptionObjects(rs.getString("options_json")),
        parseStringList(rs.getString("answer_json")),
        parseSubQuestions(rs.getString("sub_questions_json")),
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

    public PageResult<MathQuestionListItem> list(String keyword, Long bookId, Long chapterId, Long sectionId, Integer page, Integer size) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.min(Math.max(size == null ? 20 : size, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        ArrayList<Object> args = new ArrayList<>();
        appendChapterFilter(where, args, bookId, chapterId, sectionId);
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

    private void appendChapterFilter(StringBuilder where, List<Object> args, Long bookId, Long chapterId, Long sectionId) {
        if (sectionId != null) {
            SectionPath sectionPath = resolveSectionPath(sectionId);
            if (sectionPath == null) {
                where.append(" AND 1 = 0");
                return;
            }
            where.append(" AND book_name = ? AND chapter_name = ? AND section_name = ?");
            args.add(sectionPath.bookTitle());
            args.add(sectionPath.chapterTitle());
            args.add(sectionPath.sectionTitle());
            return;
        }

        if (chapterId != null) {
            ChapterPath chapterPath = resolveChapterPath(chapterId);
            if (chapterPath == null) {
                where.append(" AND 1 = 0");
                return;
            }
            where.append(" AND book_name = ? AND chapter_name = ?");
            args.add(chapterPath.bookTitle());
            args.add(chapterPath.chapterTitle());
            return;
        }

        if (bookId != null) {
            String bookTitle = resolveBookTitle(bookId);
            if (bookTitle == null) {
                where.append(" AND 1 = 0");
                return;
            }
            where.append(" AND book_name = ?");
            args.add(bookTitle);
        }
    }

    public MathQuestionDetail get(Long id) {
        var rows = jdbcTemplate.query(
            """
                SELECT id, image_url, raw_text_latex, answer_latex, teacher_explanation,
                       question_type, options_json, answer_json, sub_questions_json,
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
        validateQuestionRequest(request);
        LocalDateTime now = LocalDateTime.now();
        int questionNo = nextQuestionNo(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO math_questions(
                        image_url, question_type, raw_text_latex, options_json, answer_json, sub_questions_json, answer_latex, teacher_explanation,
                        book_name, chapter_name, section_name, source_year, source_paper,
                        question_no, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                Statement.RETURN_GENERATED_KEYS
            );
            bindRequest(ps, request, questionNo);
            ps.setTimestamp(15, Timestamp.valueOf(now));
            ps.setTimestamp(16, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long id = extractId(keyHolder, "Create math question failed");
        syncRuntimeQuestion(id, request, questionNo, now);
        return get(id);
    }

    public MathQuestionDetail update(Long id, MathQuestionRequest request) {
        validateQuestionRequest(request);
        LocalDateTime now = LocalDateTime.now();
        int questionNo = requireExistingQuestionNo(id);
        int affected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    UPDATE math_questions
                    SET image_url = ?, question_type = ?, raw_text_latex = ?, options_json = ?, answer_json = ?, sub_questions_json = ?,
                        answer_latex = ?, teacher_explanation = ?, book_name = ?, chapter_name = ?, section_name = ?, source_year = ?, source_paper = ?,
                        question_no = ?, updated_at = ?
                    WHERE id = ?
                    """
            );
            bindRequest(ps, request, questionNo);
            ps.setTimestamp(15, Timestamp.valueOf(now));
            ps.setLong(16, id);
            return ps;
        });
        if (affected == 0) {
            throw new AppException(404, "Question not found");
        }
        syncRuntimeQuestion(id, request, questionNo, now);
        return get(id);
    }

    public void delete(Long id) {
        int affected = jdbcTemplate.update("DELETE FROM math_questions WHERE id = ?", id);
        if (affected == 0) {
            throw new AppException(404, "Question not found");
        }
        jdbcTemplate.update("UPDATE questions SET import_status = 'REMOVED' WHERE source_math_question_id = ?", id);
    }

    public void syncExistingRuntimeQuestions() {
        List<Long> mathQuestionIds = jdbcTemplate.query(
            "SELECT id FROM math_questions ORDER BY id ASC",
            (rs, rowNum) -> rs.getLong("id")
        );
        for (Long mathQuestionId : mathQuestionIds) {
            MathQuestionDetail detail = get(mathQuestionId);
            try {
                syncRuntimeQuestion(
                    mathQuestionId,
                    toRequest(detail),
                    detail.questionNo(),
                    detail.updatedAt() == null ? LocalDateTime.now() : detail.updatedAt()
                );
            } catch (AppException ex) {
                if (ex.getStatus() == 400 && "Chapter tree mapping not found".equals(ex.getMessage())) {
                    log.warn("Skip runtime sync for math question {} because chapter mapping is missing", mathQuestionId);
                    continue;
                }
                throw ex;
            }
        }
    }

    private void syncRuntimeQuestion(Long mathQuestionId, MathQuestionRequest request, int questionNo, LocalDateTime now) {
        Long sectionId = resolveSectionId(request);
        String questionType = normalizeQuestionType(request.questionType());
        String sourceLabel = buildSourceLabel(request.sourceYear(), request.sourcePaper(), questionNo);
        String strippedContent = stripSourcePrefix(request.rawTextLatex().trim());
        String title = preview(strippedContent);
        String content = strippedContent;
        String explanation = blankToNull(request.teacherExplanation());
        String answerJson = buildRuntimeAnswerJson(request, questionType);
        String optionsJson = buildRuntimeOptionsJson(request, questionType);
        String subQuestionsJson = serializeSubQuestions(normalizeSubQuestions(request.subQuestions(), questionType));
        String explanationSource = explanation == null ? "NONE" : "TEACHER_GENERATED";

        List<Long> runtimeRows = jdbcTemplate.query(
            "SELECT id FROM questions WHERE source_math_question_id = ? ORDER BY id ASC",
            (rs, rowNum) -> rs.getLong("id"),
            mathQuestionId
        );
        if (runtimeRows.size() > 1) {
            throw new AppException(500, "Runtime question mapping is duplicated");
        }

        if (runtimeRows.isEmpty()) {
            jdbcTemplate.update(
                """
                    INSERT INTO questions(
                        source_math_question_id, chapter_id, title, content, type, options_json, answer_json, explanation, difficulty,
                        sub_questions_json,
                        source_year, source_paper, source_question_no, source_label, source_snapshot_path, exam_section,
                        import_batch, import_status, explanation_source, explanation_review_status, created_at
                    ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """,
                mathQuestionId,
                sectionId,
                title,
                content,
                questionType,
                optionsJson,
                answerJson,
                explanation,
                "MEDIUM",
                subQuestionsJson,
                request.sourceYear(),
                blankToNull(request.sourcePaper()),
                String.valueOf(questionNo),
                sourceLabel,
                blankToNull(request.imageUrl()),
                request.sectionName().trim(),
                "ADMIN_SYNC",
            "READY",
            explanationSource,
            "PENDING_REVIEW",
            Timestamp.valueOf(now)
        );
            assertRuntimeQuestionMapping(mathQuestionId);
            return;
        }

        jdbcTemplate.update(
            """
                UPDATE questions
                SET chapter_id = ?, title = ?, content = ?, type = ?, options_json = ?, answer_json = ?, explanation = ?, difficulty = ?,
                    sub_questions_json = ?,
                    source_year = ?, source_paper = ?, source_question_no = ?, source_label = ?, source_snapshot_path = ?, exam_section = ?,
                    import_batch = ?, import_status = ?, explanation_source = ?, explanation_review_status = ?
                WHERE source_math_question_id = ?
                """,
            sectionId,
            title,
            content,
            questionType,
            optionsJson,
            answerJson,
            explanation,
            "MEDIUM",
            subQuestionsJson,
            request.sourceYear(),
            blankToNull(request.sourcePaper()),
            String.valueOf(questionNo),
            sourceLabel,
            blankToNull(request.imageUrl()),
            request.sectionName().trim(),
            "ADMIN_SYNC",
            "READY",
            explanationSource,
            "PENDING_REVIEW",
            mathQuestionId
        );
        assertRuntimeQuestionMapping(mathQuestionId);
    }

    private void assertRuntimeQuestionMapping(Long mathQuestionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM questions WHERE source_math_question_id = ? AND import_status = 'READY'",
            Integer.class,
            mathQuestionId
        );
        if (count == null || count != 1) {
            throw new AppException(500, "Runtime question sync failed");
        }
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

    private String resolveBookTitle(Long bookId) {
        List<String> rows = jdbcTemplate.query(
            "SELECT title FROM chapters WHERE id = ?",
            (rs, rowNum) -> rs.getString("title"),
            bookId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ChapterPath resolveChapterPath(Long chapterId) {
        List<ChapterPath> rows = jdbcTemplate.query(
            """
                SELECT b.title AS book_title, c.title AS chapter_title
                FROM chapters c
                JOIN chapters b ON c.parent_id = b.id
                WHERE c.id = ?
                LIMIT 1
                """,
            (rs, rowNum) -> new ChapterPath(
                rs.getString("book_title"),
                rs.getString("chapter_title")
            ),
            chapterId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private SectionPath resolveSectionPath(Long sectionId) {
        List<SectionPath> rows = jdbcTemplate.query(
            """
                SELECT b.title AS book_title, c.title AS chapter_title, s.title AS section_title
                FROM chapters s
                JOIN chapters c ON s.parent_id = c.id
                JOIN chapters b ON c.parent_id = b.id
                WHERE s.id = ?
                LIMIT 1
                """,
            (rs, rowNum) -> new SectionPath(
                rs.getString("book_title"),
                rs.getString("chapter_title"),
                rs.getString("section_title")
            ),
            sectionId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private MathQuestionRequest toRequest(MathQuestionDetail detail) {
        return new MathQuestionRequest(
            detail.imageUrl(),
            detail.questionType(),
            detail.rawTextLatex(),
            detail.options(),
            detail.answers(),
            detail.subQuestions(),
            detail.answerLatex(),
            detail.teacherExplanation(),
            detail.bookName(),
            detail.chapterName(),
            detail.sectionName(),
            detail.sourceYear(),
            detail.sourcePaper(),
            detail.questionNo()
        );
    }

    private void bindRequest(PreparedStatement ps, MathQuestionRequest request, int questionNo) throws java.sql.SQLException {
        ps.setString(1, normalizeImageUrl(request.imageUrl()));
        ps.setString(2, normalizeQuestionType(request.questionType()));
        ps.setString(3, request.rawTextLatex().trim());
        ps.setString(4, serializeOptionObjects(normalizeOptions(request.options(), request.questionType())));
        ps.setString(5, buildStoredAnswerJson(request));
        ps.setString(6, serializeSubQuestions(normalizeSubQuestions(request.subQuestions(), request.questionType())));
        ps.setString(7, blankToNull(request.answerLatex()));
        ps.setString(8, blankToNull(request.teacherExplanation()));
        ps.setString(9, request.bookName().trim());
        ps.setString(10, request.chapterName().trim());
        ps.setString(11, request.sectionName().trim());
        if (request.sourceYear() == null) {
            ps.setObject(12, null);
        } else {
            ps.setInt(12, request.sourceYear());
        }
        ps.setString(13, blankToNull(request.sourcePaper()));
        ps.setInt(14, questionNo);
    }

    private int nextQuestionNo(MathQuestionRequest request) {
        Integer max = jdbcTemplate.queryForObject(
            """
                SELECT MAX(question_no)
                FROM math_questions
                WHERE book_name = ? AND chapter_name = ? AND section_name = ?
                """,
            Integer.class,
            request.bookName().trim(),
            request.chapterName().trim(),
            request.sectionName().trim()
        );
        return (max == null ? 0 : max) + 1;
    }

    private int requireExistingQuestionNo(Long id) {
        List<Integer> rows = jdbcTemplate.query(
            "SELECT question_no FROM math_questions WHERE id = ?",
            (rs, rowNum) -> rs.getInt("question_no"),
            id
        );
        if (rows.isEmpty()) {
            throw new AppException(404, "Question not found");
        }
        return rows.get(0);
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

    private void validateQuestionRequest(MathQuestionRequest request) {
        String questionType = normalizeQuestionType(request.questionType());
        if ("SINGLE".equals(questionType) || "MULTI".equals(questionType)) {
            List<QuestionOptionPayload> options = normalizeOptions(request.options(), questionType);
            if (options.size() < 2 || options.size() > 6) {
                throw new AppException(400, "选择题选项数量必须在 2 到 6 项之间");
            }
            List<String> answers = normalizeExplicitAnswers(request.answers());
            if (answers.isEmpty()) {
                throw new AppException(400, "选择题至少需要一个正确答案");
            }
            if ("SINGLE".equals(questionType) && answers.size() != 1) {
                throw new AppException(400, "单选题必须且只能有一个正确答案");
            }
            Set<String> optionKeys = new LinkedHashSet<>();
            for (QuestionOptionPayload option : options) {
                optionKeys.add(normalizeOptionKey(option.key()));
            }
            for (String answer : answers) {
                if (!optionKeys.contains(answer)) {
                    throw new AppException(400, "选择题答案必须对应已有选项");
                }
            }
            return;
        }

        if ("FILL".equals(questionType)) {
            if (normalizeExplicitAnswers(request.answers()).isEmpty()) {
                throw new AppException(400, "填空题至少需要一个正确答案");
            }
            return;
        }

        List<QuestionSubQuestionPayload> subQuestions = normalizeSubQuestions(request.subQuestions(), questionType);
        if (subQuestions.isEmpty()) {
            throw new AppException(400, "解答题至少需要一个小问");
        }
    }

    private String normalizeQuestionType(String rawType) {
        String normalized = rawType == null ? "" : rawType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SINGLE", "MULTI", "FILL", "SOLUTION" -> normalized;
            default -> throw new AppException(400, "questionType must be SINGLE|MULTI|FILL|SOLUTION");
        };
    }

    private List<QuestionOptionPayload> normalizeOptions(List<QuestionOptionPayload> options, String questionTypeRaw) {
        String questionType = normalizeQuestionType(questionTypeRaw);
        if (!"SINGLE".equals(questionType) && !"MULTI".equals(questionType)) {
            return Collections.emptyList();
        }
        List<QuestionOptionPayload> safeOptions = options == null ? Collections.emptyList() : options;
        List<QuestionOptionPayload> normalized = new ArrayList<>();
        Set<String> usedKeys = new LinkedHashSet<>();
        for (QuestionOptionPayload option : safeOptions) {
            if (option == null) {
                continue;
            }
            String key = normalizeOptionKey(option.key());
            String content = option.content() == null ? "" : option.content().trim();
            if (key.isBlank() || content.isBlank()) {
                continue;
            }
            if (!usedKeys.add(key)) {
                throw new AppException(400, "选择题选项标识不能重复");
            }
            normalized.add(new QuestionOptionPayload(key, content));
        }
        return normalized;
    }

    private String normalizeOptionKey(String value) {
        String key = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!key.matches("[A-Z]")) {
            throw new AppException(400, "选择题选项标识必须是单个大写字母");
        }
        return key;
    }

    private List<String> normalizeExplicitAnswers(List<String> answers) {
        List<String> normalized = new ArrayList<>();
        if (answers == null) {
            return normalized;
        }
        for (String answer : answers) {
            if (answer == null) {
                continue;
            }
            String value = answer.trim();
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private List<QuestionSubQuestionPayload> normalizeSubQuestions(List<QuestionSubQuestionPayload> subQuestions, String questionTypeRaw) {
        String questionType = normalizeQuestionType(questionTypeRaw);
        if (!"SOLUTION".equals(questionType)) {
            return Collections.emptyList();
        }
        List<QuestionSubQuestionPayload> safeSubQuestions = subQuestions == null ? Collections.emptyList() : subQuestions;
        List<QuestionSubQuestionPayload> normalized = new ArrayList<>();
        int nextIndex = 1;
        for (QuestionSubQuestionPayload subQuestion : safeSubQuestions) {
            if (subQuestion == null) {
                continue;
            }
            String prompt = blankToNull(subQuestion.prompt());
            if (prompt == null) {
                continue;
            }
            normalized.add(new QuestionSubQuestionPayload(
                nextIndex++,
                prompt,
                blankToNull(subQuestion.referenceAnswer()),
                List.of()
            ));
        }
        return normalized;
    }

    private String buildStoredAnswerJson(MathQuestionRequest request) {
        String questionType = normalizeQuestionType(request.questionType());
        if ("SOLUTION".equals(questionType)) {
            List<String> answers = new ArrayList<>();
            for (QuestionSubQuestionPayload subQuestion : normalizeSubQuestions(request.subQuestions(), questionType)) {
                String referenceAnswer = blankToNull(subQuestion.referenceAnswer());
                if (referenceAnswer != null) {
                    answers.add("（" + subQuestion.index() + "）" + referenceAnswer);
                }
            }
            return serializeAnswerList(answers);
        }

        if ("SINGLE".equals(questionType) || "MULTI".equals(questionType)) {
            List<String> answers = new ArrayList<>();
            for (String answer : normalizeExplicitAnswers(request.answers())) {
                answers.add(answer.trim().toUpperCase(Locale.ROOT));
            }
            return serializeAnswerList(answers);
        }

        return serializeAnswerList(normalizeExplicitAnswers(request.answers()));
    }

    private String buildRuntimeAnswerJson(MathQuestionRequest request, String questionType) {
        return buildStoredAnswerJson(request);
    }

    private String buildRuntimeOptionsJson(MathQuestionRequest request, String questionType) {
        if (!"SINGLE".equals(questionType) && !"MULTI".equals(questionType)) {
            return null;
        }
        List<String> formatted = new ArrayList<>();
        for (QuestionOptionPayload option : normalizeOptions(request.options(), questionType)) {
            formatted.add(option.key() + ". " + option.content());
        }
        return serializeAnswerList(formatted);
    }

    private String serializeOptionObjects(List<QuestionOptionPayload> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception ex) {
            throw new AppException(500, "Serialize options failed");
        }
    }

    private List<QuestionOptionPayload> parseOptionObjects(String rawJson) {
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

    private String serializeSubQuestions(List<QuestionSubQuestionPayload> subQuestions) {
        if (subQuestions == null || subQuestions.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(subQuestions);
        } catch (Exception ex) {
            throw new AppException(500, "Serialize sub questions failed");
        }
    }

    private List<QuestionSubQuestionPayload> parseSubQuestions(String rawJson) {
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

    private List<String> parseStringList(String rawJson) {
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

    private record ChapterPath(String bookTitle, String chapterTitle) {
    }

    private record SectionPath(String bookTitle, String chapterTitle, String sectionTitle) {
    }
}
