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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MistakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listShouldReturnQuestionDetailFieldsWhenQuestionExists() throws Exception {
        String username = "mistake_detail_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);

        long questionId = 99001L;
        jdbcTemplate.update("DELETE FROM mistake_records WHERE user_id = ?", userId);
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
                4L,
                "2016 年 408 真题 第 9 题",
                "函数 f(x)=1/(x-2) 的定义域是？",
                "SINGLE",
                "[\"A. x>2\",\"B. x<2\",\"C. x≠2\",\"D. x=2\"]",
                "[\"C\"]",
                "分母不能为 0，所以 x≠2。",
                "MEDIUM",
                "2016 年 408 真题 第 9 题",
                "READY"
            );
            jdbcTemplate.update(
                """
                    INSERT INTO mistake_records(user_id, question_id, chapter_id, difficulty, question_title, question_content, image_url, status, created_at, updated_at)
                    VALUES(?,?,?,?,?,?,?,?,?,?)
                    """,
                userId,
                questionId,
                4L,
                "MEDIUM",
                "2016 年 408 真题 第 9 题",
                "函数 f(x)=1/(x-2) 的定义域是？",
                null,
                "REVIEWING",
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
            );

            mockMvc.perform(get("/api/mistakes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].questionTitle").value("2016 年 408 真题 第 9 题"))
                .andExpect(jsonPath("$.data[0].questionType").value("SINGLE"))
                .andExpect(jsonPath("$.data[0].options[0]").value("A. x>2"))
                .andExpect(jsonPath("$.data[0].correctAnswer").value("C"))
                .andExpect(jsonPath("$.data[0].explanation").value("分母不能为 0，所以 x≠2。"))
                .andExpect(jsonPath("$.data[0].sourceLabel").value("2016 年 408 真题 第 9 题"));
        } finally {
            jdbcTemplate.update("DELETE FROM mistake_records WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", questionId);
        }
    }

    @Test
    void listShouldKeepLegacyMistakesReadableWhenQuestionIdIsNull() throws Exception {
        String username = "mistake_legacy_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);

        jdbcTemplate.update("DELETE FROM mistake_records WHERE user_id = ?", userId);
        try {
            jdbcTemplate.update(
                """
                    INSERT INTO mistake_records(user_id, question_id, chapter_id, difficulty, question_title, question_content, image_url, status, created_at, updated_at)
                    VALUES(?,?,?,?,?,?,?,?,?,?)
                    """,
                userId,
                null,
                4L,
                null,
                "旧错题",
                "旧错题内容",
                null,
                "REVIEWING",
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
            );

            mockMvc.perform(get("/api/mistakes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].questionTitle").value("旧错题"))
                .andExpect(jsonPath("$.data[0].questionType").value(nullValue()))
                .andExpect(jsonPath("$.data[0].options").isArray())
                .andExpect(jsonPath("$.data[0].options.length()").value(0))
                .andExpect(jsonPath("$.data[0].correctAnswer").value(nullValue()))
                .andExpect(jsonPath("$.data[0].explanation").value(nullValue()))
                .andExpect(jsonPath("$.data[0].sourceLabel").value(nullValue()));
        } finally {
            jdbcTemplate.update("DELETE FROM mistake_records WHERE user_id = ?", userId);
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
