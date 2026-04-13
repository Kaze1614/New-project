package com.datong.mathai;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QaContextTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void qaMessageWithContextShouldWork() throws Exception {
        String username = "qa_" + System.currentTimeMillis();
        String token = registerAndGetToken(username);

        MvcResult sessionResult = mockMvc.perform(post("/api/qa/sessions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "上下文会话"))))
            .andExpect(status().isOk())
            .andReturn();
        long sessionId = objectMapper.readTree(sessionResult.getResponse().getContentAsString())
            .path("data")
            .path("id")
            .asLong();

        mockMvc.perform(post("/api/qa/sessions/{id}/messages", sessionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "content", "请解释这道题",
                    "context", "题目：f(x)=x^2；解析：导数为2x。"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userMessage.content").value("请解释这道题"))
            .andExpect(jsonPath("$.data.assistantMessage.content").isNotEmpty());
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
