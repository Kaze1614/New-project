package com.datong.mathai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudyFlowTest {

    private static final long SECTION_ID = 4L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void studySubmitShouldWriteMistakeAndReviewTask() throws Exception {
        seedRuntimeQuestions(20);
        String username = "study_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);

        MvcResult sessionResult = mockMvc.perform(post("/api/study/sessions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("chapterId", SECTION_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCount").value(20))
            .andReturn();

        JsonNode sessionNode = objectMapper.readTree(sessionResult.getResponse().getContentAsString()).path("data");
        long sessionId = sessionNode.path("id").asLong();
        long firstItemId = sessionNode.path("items").get(0).path("itemId").asLong();

        mockMvc.perform(post("/api/study/sessions/{id}/answers", sessionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "itemId", firstItemId,
                    "answer", "WRONG_ANSWER"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.answeredCount").value(1));

        mockMvc.perform(post("/api/study/sessions/{id}/submit", sessionId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.submitted").value(true));

        MvcResult reviewResult = mockMvc.perform(get("/api/review/tasks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode reviewNode = objectMapper.readTree(reviewResult.getResponse().getContentAsString()).path("data");
        assertTrue(reviewNode.isArray());
        assertTrue(reviewNode.size() >= 1);

        int questionBankTotal = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM questions WHERE import_status = 'READY'",
            Integer.class
        );
        int unmappedReadyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM questions WHERE import_status = 'READY' AND source_math_question_id IS NULL",
            Integer.class
        );

        assertTrue(unmappedReadyCount == 0);

        mockMvc.perform(get("/api/dashboard/overview")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionBankTotal").value(questionBankTotal))
            .andExpect(jsonPath("$.data.totalMistakes").value(1))
            .andExpect(jsonPath("$.data.pendingReview").value(1));
    }

    private String registerAndGetToken(String username) throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", "123456"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(registerResult.getResponse().getContentAsString())
            .path("data")
            .path("token")
            .asText();
    }

    private void seedRuntimeQuestions(int count) {
        jdbcTemplate.update("DELETE FROM questions WHERE source_math_question_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM math_questions");

        LocalDateTime now = LocalDateTime.now();
        for (int index = 1; index <= count; index++) {
            long mathQuestionId = insertMathQuestion(index, now);
            insertRuntimeQuestion(mathQuestionId, index, now);
        }
    }

    private long insertMathQuestion(int questionNo, LocalDateTime now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO math_questions(
                        image_url, question_type, raw_text_latex, options_json, answer_json, sub_questions_json, answer_latex,
                        teacher_explanation, book_name, chapter_name, section_name, source_year, source_paper, question_no,
                        created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, "");
            ps.setString(2, "SINGLE");
            ps.setString(3, "Question " + questionNo + ": choose the correct answer.");
            ps.setString(4, """
                [{"key":"A","content":"Option A"},{"key":"B","content":"Option B"},{"key":"C","content":"Option C"},{"key":"D","content":"Option D"}]
                """.trim());
            ps.setString(5, "[\"B\"]");
            ps.setObject(6, null);
            ps.setString(7, "B");
            ps.setString(8, "Explanation for question " + questionNo);
            ps.setString(9, "Book 1");
            ps.setString(10, "Chapter 1");
            ps.setString(11, "Section 1");
            ps.setInt(12, 2026);
            ps.setString(13, "StudyFlow");
            ps.setInt(14, questionNo);
            ps.setTimestamp(15, Timestamp.valueOf(now));
            ps.setTimestamp(16, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);
        return extractId(keyHolder);
    }

    private void insertRuntimeQuestion(long mathQuestionId, int questionNo, LocalDateTime now) {
        jdbcTemplate.update(
            """
                INSERT INTO questions(
                    source_math_question_id, chapter_id, title, content, type, options_json, answer_json, sub_questions_json,
                    explanation, difficulty, source_year, source_paper, source_question_no, source_label, exam_section,
                    import_batch, import_status, explanation_source, explanation_review_status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            mathQuestionId,
            SECTION_ID,
            "Question " + questionNo,
            questionNo + ".(2026)(StudyFlow) Question " + questionNo + ": choose the correct answer.",
            "SINGLE",
            "[\"A. Option A\",\"B. Option B\",\"C. Option C\",\"D. Option D\"]",
            "[\"B\"]",
            null,
            "Explanation for question " + questionNo,
            "MEDIUM",
            2026,
            "StudyFlow",
            String.valueOf(questionNo),
            questionNo + ".(2026)(StudyFlow)",
            "Section 1",
            "TEST_SYNC",
            "READY",
            "TEACHER_GENERATED",
            "PENDING_REVIEW",
            Timestamp.valueOf(now)
        );
    }

    private long extractId(KeyHolder keyHolder) {
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
            Object firstValue = keys.values().stream().findFirst().orElseThrow();
            if (firstValue instanceof Number number) {
                return number.longValue();
            }
        }
        throw new IllegalStateException("Failed to extract generated id");
    }
}
