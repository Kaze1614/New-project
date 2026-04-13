package com.datong.mathai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

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

    @Test
    void reviewTaskShouldSupportAgainHardEasy() throws Exception {
        String username = "review_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);
        long taskId = createTaskByMistakeAnalyze(token);

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "again"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastGrade").value("AGAIN"))
            .andExpect(jsonPath("$.data.completed").value(false))
            .andExpect(jsonPath("$.data.suspended").value(false));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "hard"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastGrade").value("HARD"))
            .andExpect(jsonPath("$.data.intervalDays").value(3));

        mockMvc.perform(post("/api/review/tasks/{id}/rate", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("grade", "easy"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lastGrade").value("EASY"))
            .andExpect(jsonPath("$.data.completed").value(true))
            .andExpect(jsonPath("$.data.suspended").value(true));

        mockMvc.perform(get("/api/review/next")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
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

    private long createTaskByMistakeAnalyze(String token) throws Exception {
        MvcResult createMistakeResult = mockMvc.perform(post("/api/mistakes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "chapterId", 4,
                    "questionTitle", "复习调度测试题",
                    "questionContent", "测试内容"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        long mistakeId = objectMapper.readTree(createMistakeResult.getResponse().getContentAsString())
            .path("data")
            .path("id")
            .asLong();

        mockMvc.perform(post("/api/mistakes/{id}/analyze", mistakeId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        MvcResult reviewList = mockMvc.perform(get("/api/review/tasks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode task = objectMapper.readTree(reviewList.getResponse().getContentAsString()).path("data").get(0);
        return task.path("id").asLong();
    }
}
