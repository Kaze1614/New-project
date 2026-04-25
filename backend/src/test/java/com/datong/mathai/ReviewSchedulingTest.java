package com.datong.mathai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reviewTaskShouldFollowLeitnerBoxes() throws Exception {
        String username = "review_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);
        long taskId = createReviewTasksByStudySubmit(token, 1).get(0);

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "hard"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastGrade").value("HARD"))
            .andExpect(jsonPath("$.data.repetition").value(2))
            .andExpect(jsonPath("$.data.intervalDays").value(3))
            .andExpect(jsonPath("$.data.completed").value(false))
            .andExpect(jsonPath("$.data.suspended").value(false));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "easy"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastGrade").value("EASY"))
            .andExpect(jsonPath("$.data.repetition").value(3))
            .andExpect(jsonPath("$.data.intervalDays").value(7))
            .andExpect(jsonPath("$.data.completed").value(false))
            .andExpect(jsonPath("$.data.suspended").value(false));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "again"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastGrade").value("AGAIN"))
            .andExpect(jsonPath("$.data.repetition").value(1))
            .andExpect(jsonPath("$.data.intervalDays").value(1))
            .andExpect(jsonPath("$.data.completed").value(false))
            .andExpect(jsonPath("$.data.suspended").value(false));

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
        markTasksDue(taskIds);

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
        MvcResult sessionResult = mockMvc.perform(post("/api/study/sessions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("chapterId", 4))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode items = objectMapper.readTree(sessionResult.getResponse().getContentAsString()).path("data").path("items");
        long sessionId = objectMapper.readTree(sessionResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        for (int i = 0; i < wrongCount; i++) {
            long itemId = items.get(i).path("itemId").asLong();
            mockMvc.perform(post("/api/study/sessions/{id}/answers", sessionId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "itemId", itemId,
                        "answer", "WRONG_ANSWER_" + i
                    ))))
                .andExpect(status().isOk());
        }

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

    private void markTasksDue(List<Long> taskIds) {
        for (Long taskId : taskIds) {
            jdbcTemplate.update(
                "UPDATE review_tasks SET due_date = ?, repetition = 1, interval_days = 1 WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(5)),
                taskId
            );
        }
    }
}
