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
class LearningFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerToAnalyzeToReviewToDashboardShouldWork() throws Exception {
        String username = "flow_" + System.currentTimeMillis();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", "123456"
                ))))
            .andExpect(status().isOk())
            .andReturn();

        String registerBody = registerResult.getResponse().getContentAsString();
        JsonNode registerNode = objectMapper.readTree(registerBody);
        String token = registerNode.path("data").path("token").asText();

        MvcResult createMistakeResult = mockMvc.perform(post("/api/mistakes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "chapterId", 4,
                    "questionTitle", "极限流程测试",
                    "questionContent", "求 lim(x->1) (x^2-1)/(x-1)",
                    "imageUrl", "https://example.com/m1.png"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionTitle").value("极限流程测试"))
            .andReturn();

        JsonNode createNode = objectMapper.readTree(createMistakeResult.getResponse().getContentAsString());
        long mistakeId = createNode.path("data").path("id").asLong();

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

        JsonNode reviewNode = objectMapper.readTree(reviewListResult.getResponse().getContentAsString());
        long taskId = reviewNode.path("data").get(0).path("id").asLong();

        mockMvc.perform(post("/api/review/tasks/{id}/complete", taskId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.completed").value(true));

        mockMvc.perform(get("/api/dashboard/overview")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalMistakes").value(1))
            .andExpect(jsonPath("$.data.mastered").value(1));

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + token)
                .param("keyword", "极限"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questions").isArray())
            .andExpect(jsonPath("$.data.mistakes").isArray());
    }
}
