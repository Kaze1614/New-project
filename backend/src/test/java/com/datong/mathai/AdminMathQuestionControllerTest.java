package com.datong.mathai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
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

        Map<String, String> createSection = loadChapterPath(1L, 2L, 3L);
        String createRawText = "1.(2026)(Test Paper) Set A={1,2}. How many elements are in A?";

        MvcResult createResult = mockMvc.perform(post("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "imageUrl", "/uploads/questions/demo.png",
                    "rawTextLatex", createRawText,
                    "answerLatex", "2",
                    "teacherExplanation", "Set A contains two elements.",
                    "bookName", createSection.get("book"),
                    "chapterName", createSection.get("chapter"),
                    "sectionName", createSection.get("section"),
                    "sourceYear", 2026,
                    "sourcePaper", "Test Paper",
                    "questionNo", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").isNumber())
            .andExpect(jsonPath("$.data.sourceLabel").value("1.(2026)(Test Paper)"))
            .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = createNode.path("data").path("id").asLong();

        mockMvc.perform(get("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .param("keyword", "elements")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(id))
            .andExpect(jsonPath("$.data.items[0].contentPreview").isNotEmpty());

        mockMvc.perform(get("/api/admin/math-questions/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rawTextLatex").value(createRawText));

        Integer syncedQuestionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM questions WHERE source_math_question_id = ? AND import_status = 'READY'",
            Integer.class,
            id
        );
        Assertions.assertEquals(1, syncedQuestionCount);

        Map<String, String> updateSection = loadChapterPath(1L, 12L, 13L);
        String updateRawText = "2.(2026)(Test Paper) Given f(x)=x+1, find f(2).";

        mockMvc.perform(put("/api/admin/math-questions/{id}", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "imageUrl", "/uploads/questions/demo.png",
                    "rawTextLatex", updateRawText,
                    "answerLatex", "3",
                    "teacherExplanation", "Substitute x=2 and get 3.",
                    "bookName", updateSection.get("book"),
                    "chapterName", updateSection.get("chapter"),
                    "sectionName", updateSection.get("section"),
                    "sourceYear", 2026,
                    "sourcePaper", "Test Paper",
                    "questionNo", 2
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionNo").value(2))
            .andExpect(jsonPath("$.data.answerLatex").value("3"));

        mockMvc.perform(delete("/api/admin/math-questions/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        String importStatus = jdbcTemplate.queryForObject(
            "SELECT import_status FROM questions WHERE source_math_question_id = ?",
            String.class,
            id
        );
        Assertions.assertEquals("REMOVED", importStatus);

        mockMvc.perform(get("/api/admin/math-questions/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    private Map<String, String> loadChapterPath(Long bookId, Long chapterId, Long sectionId) {
        return jdbcTemplate.queryForObject(
            """
                SELECT b.title AS book_title, c.title AS chapter_title, s.title AS section_title
                FROM chapters s
                JOIN chapters c ON s.parent_id = c.id
                JOIN chapters b ON c.parent_id = b.id
                WHERE b.id = ? AND c.id = ? AND s.id = ?
                """,
            (rs, rowNum) -> Map.of(
                "book", rs.getString("book_title"),
                "chapter", rs.getString("chapter_title"),
                "section", rs.getString("section_title")
            ),
            bookId,
            chapterId,
            sectionId
        );
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
