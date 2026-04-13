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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudyFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void studySubmitShouldWriteMistakeAndReviewTask() throws Exception {
        String username = "study_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);

        MvcResult sessionResult = mockMvc.perform(post("/api/study/sessions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("chapterId", 4))))
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

        mockMvc.perform(get("/api/dashboard/overview")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionBankTotal").value(20))
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
}
