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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LearningFlowTest {

    private static final long SECTION_ID = 4L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerToAnalyzeToReviewToDashboardShouldWork() throws Exception {
        seedRuntimeQuestion(9001);
        String username = "flow_" + System.currentTimeMillis();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", "123456"
                ))))
            .andExpect(status().isOk())
            .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
            .path("data")
            .path("token")
            .asText();

        MvcResult createMistakeResult = mockMvc.perform(post("/api/mistakes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "chapterId", SECTION_ID,
                    "questionTitle", "Limit practice",
                    "questionContent", "Compute lim(x->1) (x^2-1)/(x-1).",
                    "imageUrl", "https://example.com/m1.png"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionTitle").value("Limit practice"))
            .andReturn();

        long mistakeId = objectMapper.readTree(createMistakeResult.getResponse().getContentAsString())
            .path("data")
            .path("id")
            .asLong();

        mockMvc.perform(post("/api/mistakes/{id}/analyze", mistakeId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.errorType").isNotEmpty())
            .andExpect(jsonPath("$.data.knowledgePoints").isArray());

        MvcResult reviewListResult = mockMvc.perform(get("/api/review/tasks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].mistakeId").value(mistakeId))
            .andReturn();

        long taskId = objectMapper.readTree(reviewListResult.getResponse().getContentAsString())
            .path("data")
            .get(0)
            .path("id")
            .asLong();

        mockMvc.perform(post("/api/review/tasks/{id}/complete", taskId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.completed").value(false))
            .andExpect(jsonPath("$.data.repetition").value(2))
            .andExpect(jsonPath("$.data.intervalDays").value(3))
            .andExpect(jsonPath("$.data.lastGrade").value("EASY"));

        int questionBankTotal = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM questions WHERE import_status = 'READY'",
            Integer.class
        );

        mockMvc.perform(get("/api/dashboard/overview")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionBankTotal").value(questionBankTotal))
            .andExpect(jsonPath("$.data.totalMistakes").value(1))
            .andExpect(jsonPath("$.data.mastered").value(0))
            .andExpect(jsonPath("$.data.pendingReview").value(1));

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + token)
                .param("keyword", "LimitFlow"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questions").isArray())
            .andExpect(jsonPath("$.data.questions[0].title").value("LimitFlow question"))
            .andExpect(jsonPath("$.data.mistakes").isArray());
    }

    private void seedRuntimeQuestion(int questionNo) {
        jdbcTemplate.update("DELETE FROM questions WHERE source_math_question_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM math_questions");

        LocalDateTime now = LocalDateTime.now();
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
            ps.setString(3, "LimitFlow question stem");
            ps.setString(4, """
                [{"key":"A","content":"Option A"},{"key":"B","content":"Option B"},{"key":"C","content":"Option C"},{"key":"D","content":"Option D"}]
                """.trim());
            ps.setString(5, "[\"A\"]");
            ps.setObject(6, null);
            ps.setString(7, "A");
            ps.setString(8, "LimitFlow explanation");
            ps.setString(9, "Book 1");
            ps.setString(10, "Chapter 1");
            ps.setString(11, "Section 1");
            ps.setInt(12, 2026);
            ps.setString(13, "LimitFlow");
            ps.setInt(14, questionNo);
            ps.setTimestamp(15, Timestamp.valueOf(now));
            ps.setTimestamp(16, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        long mathQuestionId = extractId(keyHolder);
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
            "LimitFlow question",
            questionNo + ".(2026)(LimitFlow) LimitFlow question content",
            "SINGLE",
            "[\"A. Option A\",\"B. Option B\",\"C. Option C\",\"D. Option D\"]",
            "[\"A\"]",
            null,
            "LimitFlow explanation",
            "MEDIUM",
            2026,
            "LimitFlow",
            String.valueOf(questionNo),
            questionNo + ".(2026)(LimitFlow)",
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
