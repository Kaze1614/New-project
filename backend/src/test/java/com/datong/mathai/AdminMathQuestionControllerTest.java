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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMathQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adminQuestionCrudShouldWork() throws Exception {
        jdbcTemplate.update("DELETE FROM math_questions");
        String token = loginAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "imageUrl", "/uploads/questions/demo.png",
                    "rawTextLatex", "1.(2026)(全国卷) 已知集合 A={1,2}，求 A 的元素个数。",
                    "answerLatex", "2",
                    "teacherExplanation", "集合 A 中有两个元素。",
                    "bookName", "必修第一册",
                    "chapterName", "第一章 集合与常用逻辑用语",
                    "sectionName", "集合的概念",
                    "sourceYear", 2026,
                    "sourcePaper", "全国卷",
                    "questionNo", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").isNumber())
            .andExpect(jsonPath("$.data.sourceLabel").value("1.(2026)(全国卷)"))
            .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = createNode.path("data").path("id").asLong();

        mockMvc.perform(get("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .param("keyword", "集合")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(id))
            .andExpect(jsonPath("$.data.items[0].contentPreview").isNotEmpty());

        mockMvc.perform(get("/api/admin/math-questions/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rawTextLatex").value("1.(2026)(全国卷) 已知集合 A={1,2}，求 A 的元素个数。"));

        mockMvc.perform(put("/api/admin/math-questions/{id}", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "imageUrl", "/uploads/questions/demo.png",
                    "rawTextLatex", "2.(2026)(全国卷) 已知函数 f(x)=x+1，求 f(2)。",
                    "answerLatex", "3",
                    "teacherExplanation", "代入 x=2，得到 3。",
                    "bookName", "必修第一册",
                    "chapterName", "第三章 函数的概念与性质",
                    "sectionName", "函数的概念",
                    "sourceYear", 2026,
                    "sourcePaper", "全国卷",
                    "questionNo", 2
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionNo").value(2))
            .andExpect(jsonPath("$.data.answerLatex").value("3"));

        mockMvc.perform(delete("/api/admin/math-questions/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/math-questions/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    private String loginAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "admin",
                    "password", "123456"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
    }
}
