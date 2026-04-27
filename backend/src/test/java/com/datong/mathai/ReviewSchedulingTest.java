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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewSchedulingTest {

    private static final long SECTION_ID = 4L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reviewTaskShouldCreateImmediatelyDueAndFollowLeitnerBoxes() throws Exception {
        String username = "review_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);
        long taskId = createReviewTasksByStudySubmit(token, 1).get(0);

        Timestamp dueDate = jdbcTemplate.queryForObject(
            "SELECT due_date FROM review_tasks WHERE id = ?",
            Timestamp.class,
            taskId
        );
        assertTrue(dueDate.toLocalDateTime().isBefore(LocalDateTime.now().plusSeconds(1)));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "hard"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastGrade").value("HARD"))
            .andExpect(jsonPath("$.data.repetition").value(2))
            .andExpect(jsonPath("$.data.intervalDays").value(3));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "easy"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastGrade").value("EASY"))
            .andExpect(jsonPath("$.data.repetition").value(3))
            .andExpect(jsonPath("$.data.intervalDays").value(7));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "again"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastGrade").value("AGAIN"))
            .andExpect(jsonPath("$.data.repetition").value(2))
            .andExpect(jsonPath("$.data.intervalDays").value(3));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "again"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.repetition").value(1))
            .andExpect(jsonPath("$.data.intervalDays").value(0));

        Timestamp resetDueDate = jdbcTemplate.queryForObject(
            "SELECT due_date FROM review_tasks WHERE id = ?",
            Timestamp.class,
            taskId
        );
        assertTrue(resetDueDate.toLocalDateTime().isBefore(LocalDateTime.now().plusSeconds(1)));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "hard"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.repetition").value(2));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "easy"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.repetition").value(3));

        mockMvc.perform(post("/api/review/tasks/{id}/complete", taskId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.completed").value(true))
            .andExpect(jsonPath("$.data.suspended").value(true));

        mockMvc.perform(get("/api/review/tasks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/mistakes")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void reviewSubmitShouldHandleDueTasksAndKeepUnansweredTasksUnchanged() throws Exception {
        String username = "review_submit_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);
        List<Long> taskIds = createReviewTasksByStudySubmit(token, 2);

        MvcResult dueResult = mockMvc.perform(get("/api/review/tasks")
                .header("Authorization", "Bearer " + token)
                .param("scope", "due"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].type").isNotEmpty())
            .andExpect(jsonPath("$.data[0].officialAnswer").isNotEmpty())
            .andReturn();

        JsonNode dueData = objectMapper.readTree(dueResult.getResponse().getContentAsString()).path("data");
        long firstTaskId = dueData.get(0).path("id").asLong();
        String firstAnswer = dueData.get(0).path("officialAnswer").asText();
        long secondTaskId = dueData.get(1).path("id").asLong();

        mockMvc.perform(post("/api/review/submit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "answers", List.of(Map.of("taskId", firstTaskId, "answer", firstAnswer))
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCount").value(2))
            .andExpect(jsonPath("$.data.answeredCount").value(1))
            .andExpect(jsonPath("$.data.correctCount").value(1))
            .andExpect(jsonPath("$.data.items[0].taskId").value(firstTaskId))
            .andExpect(jsonPath("$.data.items[0].answered").value(true))
            .andExpect(jsonPath("$.data.items[0].correct").value(true))
            .andExpect(jsonPath("$.data.items[0].nextBox").value(2))
            .andExpect(jsonPath("$.data.items[0].removedFromMistakes").value(false))
            .andExpect(jsonPath("$.data.items[1].taskId").value(secondTaskId))
            .andExpect(jsonPath("$.data.items[1].answered").value(false))
            .andExpect(jsonPath("$.data.items[1].nextBox").value(1));

        Integer firstBox = jdbcTemplate.queryForObject("SELECT repetition FROM review_tasks WHERE id = ?", Integer.class, firstTaskId);
        Integer secondBox = jdbcTemplate.queryForObject("SELECT repetition FROM review_tasks WHERE id = ?", Integer.class, secondTaskId);
        assertEquals(2, firstBox);
        assertEquals(1, secondBox);
        assertTrue(taskIds.size() >= 2);
    }

    @Test
    void reviewSubmitShouldDeleteMistakeWhenBoxThreeAnswerIsCorrect() throws Exception {
        String username = "review_remove_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);
        long taskId = createReviewTasksByStudySubmit(token, 1).get(0);

        Long mistakeId = jdbcTemplate.queryForObject("SELECT mistake_id FROM review_tasks WHERE id = ?", Long.class, taskId);
        jdbcTemplate.update(
            "UPDATE review_tasks SET due_date = ?, repetition = 3, interval_days = 7 WHERE id = ?",
            Timestamp.valueOf(LocalDateTime.now().minusMinutes(5)),
            taskId
        );

        MvcResult dueResult = mockMvc.perform(get("/api/review/tasks")
                .header("Authorization", "Bearer " + token)
                .param("scope", "due"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode dueItem = objectMapper.readTree(dueResult.getResponse().getContentAsString()).path("data").get(0);
        String correctAnswer = dueItem.path("officialAnswer").asText();

        mockMvc.perform(post("/api/review/submit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "answers", List.of(Map.of("taskId", taskId, "answer", correctAnswer))
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].taskId").value(taskId))
            .andExpect(jsonPath("$.data.items[0].correct").value(true))
            .andExpect(jsonPath("$.data.items[0].removedFromMistakes").value(true))
            .andExpect(jsonPath("$.data.items[0].nextBox").value(0));

        Integer mistakeCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM mistake_records WHERE id = ?", Integer.class, mistakeId);
        Integer taskCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM review_tasks WHERE id = ?", Integer.class, taskId);
        assertEquals(0, mistakeCount);
        assertEquals(0, taskCount);
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

    private List<Long> createReviewTasksByStudySubmit(String token, int wrongCount) throws Exception {
        seedRuntimeQuestions(20);

        MvcResult sessionResult = mockMvc.perform(post("/api/study/sessions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("chapterId", SECTION_ID))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode responseBody = objectMapper.readTree(sessionResult.getResponse().getContentAsString()).path("data");
        JsonNode items = responseBody.path("items");
        long sessionId = responseBody.path("id").asLong();

        int answeredWrongCount = 0;
        for (JsonNode item : items) {
            if (answeredWrongCount >= wrongCount) {
                break;
            }
            long itemId = item.path("itemId").asLong();
            mockMvc.perform(post("/api/study/sessions/{id}/answers", sessionId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "itemId", itemId,
                        "answer", "WRONG_ANSWER_" + answeredWrongCount
                    ))))
                .andExpect(status().isOk());
            answeredWrongCount++;
        }

        assertTrue(answeredWrongCount >= wrongCount);

        mockMvc.perform(post("/api/study/sessions/{id}/submit", sessionId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        MvcResult reviewList = mockMvc.perform(get("/api/review/tasks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode taskArray = objectMapper.readTree(reviewList.getResponse().getContentAsString()).path("data");
        List<Long> taskIds = new ArrayList<>();
        for (JsonNode item : taskArray) {
            taskIds.add(item.path("id").asLong());
        }
        assertTrue(taskIds.size() >= wrongCount);
        return taskIds;
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
            ps.setString(3, "Review question " + questionNo);
            ps.setString(4, """
                [{"key":"A","content":"Option A"},{"key":"B","content":"Option B"},{"key":"C","content":"Option C"},{"key":"D","content":"Option D"}]
                """.trim());
            ps.setString(5, "[\"B\"]");
            ps.setObject(6, null);
            ps.setString(7, "B");
            ps.setString(8, "Explanation for review question " + questionNo);
            ps.setString(9, "Book 1");
            ps.setString(10, "Chapter 1");
            ps.setString(11, "Section 1");
            ps.setInt(12, 2026);
            ps.setString(13, "ReviewFlow");
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
            "Review question " + questionNo,
            questionNo + ".(2026)(ReviewFlow) Review question " + questionNo,
            "SINGLE",
            "[\"A. Option A\",\"B. Option B\",\"C. Option C\",\"D. Option D\"]",
            "[\"B\"]",
            null,
            "Explanation for review question " + questionNo,
            "MEDIUM",
            2026,
            "ReviewFlow",
            String.valueOf(questionNo),
            questionNo + ".(2026)(ReviewFlow)",
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
