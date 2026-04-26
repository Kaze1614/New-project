package com.datong.mathai;

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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listShouldReturnQuestionDetailsAndSupportAncestorChapterFilter() throws Exception {
        String username = "favorite_list_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);

        long questionId = 99101L;
        jdbcTemplate.update("DELETE FROM favorites WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM questions WHERE id = ?", questionId);
        try {
            jdbcTemplate.update(
                """
                    INSERT INTO questions(
                        id, chapter_id, title, content, type, options_json, answer_json, explanation, difficulty,
                        source_label, import_status
                    ) VALUES(?,?,?,?,?,?,?,?,?,?,?)
                    """,
                questionId,
                5L,
                "极限基础判断",
                "已知 lim x→0 sinx/x = ?",
                "SINGLE",
                "[\"A. 0\",\"B. 1\",\"C. -1\",\"D. 不存在\"]",
                "[\"B\"]",
                "经典极限，结果为 1。",
                "MEDIUM",
                "教材例题",
                "READY"
            );
            jdbcTemplate.update(
                """
                    INSERT INTO favorites(user_id, question_id, chapter_id, difficulty, title, content, created_at)
                    VALUES(?,?,?,?,?,?,?)
                    """,
                userId,
                questionId,
                5L,
                "MEDIUM",
                "极限基础判断",
                "已知 lim x→0 sinx/x = ?",
                Timestamp.valueOf(LocalDateTime.now())
            );

            mockMvc.perform(get("/api/favorites")
                    .header("Authorization", "Bearer " + token)
                    .param("chapterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("极限基础判断"))
                .andExpect(jsonPath("$.data[0].questionType").value("SINGLE"))
                .andExpect(jsonPath("$.data[0].options[1]").value("B. 1"))
                .andExpect(jsonPath("$.data[0].correctAnswer").value("B"))
                .andExpect(jsonPath("$.data[0].sourceLabel").value("教材例题"));
        } finally {
            jdbcTemplate.update("DELETE FROM favorites WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", questionId);
        }
    }

    @Test
    void addToReviewShouldResetTaskToBoxOne() throws Exception {
        String username = "favorite_review_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);

        long questionId = 99102L;
        jdbcTemplate.update("DELETE FROM review_tasks WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM mistake_records WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM favorites WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM questions WHERE id = ?", questionId);
        try {
            jdbcTemplate.update(
                """
                    INSERT INTO questions(
                        id, chapter_id, title, content, type, options_json, answer_json, explanation, difficulty,
                        source_label, import_status
                    ) VALUES(?,?,?,?,?,?,?,?,?,?,?)
                    """,
                questionId,
                5L,
                "函数值计算",
                "已知 f(x)=2x+1，则 f(3)=？",
                "FILL",
                "[]",
                "[\"7\"]",
                "代入即可得到 7。",
                "EASY",
                "教材例题",
                "READY"
            );
            jdbcTemplate.update(
                """
                    INSERT INTO favorites(user_id, question_id, chapter_id, difficulty, title, content, created_at)
                    VALUES(?,?,?,?,?,?,?)
                    """,
                userId,
                questionId,
                5L,
                "EASY",
                "函数值计算",
                "已知 f(x)=2x+1，则 f(3)=？",
                Timestamp.valueOf(LocalDateTime.now())
            );

            Long favoriteId = jdbcTemplate.queryForObject(
                "SELECT id FROM favorites WHERE user_id = ? AND question_id = ?",
                Long.class,
                userId,
                questionId
            );

            mockMvc.perform(post("/api/favorites/{id}/review", favoriteId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

            Long taskId = jdbcTemplate.queryForObject(
                """
                    SELECT r.id
                    FROM review_tasks r
                    JOIN mistake_records m ON r.mistake_id = m.id
                    WHERE r.user_id = ? AND m.question_id = ?
                    """,
                Long.class,
                userId,
                questionId
            );

            jdbcTemplate.update(
                "UPDATE review_tasks SET repetition = 3, interval_days = 7, due_date = ?, completed = 1, suspended = 1, last_grade = 'EASY' WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().plusDays(7)),
                taskId
            );

            mockMvc.perform(post("/api/favorites/{id}/review", favoriteId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/review/tasks")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].questionTitle").value("函数值计算"))
                .andExpect(jsonPath("$.data[0].repetition").value(1))
                .andExpect(jsonPath("$.data[0].intervalDays").value(0))
                .andExpect(jsonPath("$.data[0].completed").value(false))
                .andExpect(jsonPath("$.data[0].suspended").value(false))
                .andExpect(jsonPath("$.data[0].lastGrade").value("FAVORITE"));
        } finally {
            jdbcTemplate.update("DELETE FROM review_tasks WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM mistake_records WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM favorites WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", questionId);
        }
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
}
