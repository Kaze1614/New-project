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

import java.util.LinkedHashMap;
import java.util.List;
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
    void adminQuestionCrudShouldSupportStructuredQuestionTypesAutoNumberingAndChapterFilters() throws Exception {
        jdbcTemplate.update("DELETE FROM math_questions");
        jdbcTemplate.update("UPDATE questions SET import_status = 'REMOVED' WHERE source_math_question_id IS NOT NULL");

        String token = loginAdmin();
        ChapterPathInfo firstSection = loadChapterPath(1L, 2L, 3L);
        ChapterPathInfo secondSection = loadAlternativeSection(firstSection.sectionId());

        long singleId = createQuestion(token, buildSinglePayload(firstSection, "鍑芥暟 y=|x| 鍏充簬鍝潯杞村绉帮紵", 2026, "妯℃嫙鍗稟"));
        long multiId = createQuestion(token, buildMultiPayload(firstSection, "涓嬪垪鍝簺寮忓瓙鎭掑ぇ浜?0锛?, 2026, "妯℃嫙鍗稟"));
        long fillId = createQuestion(token, buildFillPayload(firstSection, "宸茬煡 a+b=3锛屽垯 a+b=___銆?, 2026, "妯℃嫙鍗稟"));
        long solutionId = createQuestion(token, buildSolutionPayload(secondSection, "宸茬煡 f(x)=x^2锛屽畬鎴愪笅鍒楅棶棰樸€?, 2026, "妯℃嫙鍗稟"));

        int expectedBookCount = firstSection.bookId().equals(secondSection.bookId()) ? 4 : 3;
        int expectedChapterCount = firstSection.chapterId().equals(secondSection.chapterId()) ? 4 : 3;

        mockMvc.perform(get("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .param("keyword", "鍑芥暟 y=|x|")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(singleId))
            .andExpect(jsonPath("$.data.items[0].questionNo").value(1));

        mockMvc.perform(get("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .param("bookId", String.valueOf(firstSection.bookId()))
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(expectedBookCount));

        mockMvc.perform(get("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .param("chapterId", String.valueOf(firstSection.chapterId()))
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(expectedChapterCount));

        mockMvc.perform(get("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .param("sectionId", String.valueOf(firstSection.sectionId()))
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(3));

        mockMvc.perform(get("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .param("keyword", "鍑芥暟 y=|x|")
                .param("chapterId", String.valueOf(firstSection.chapterId()))
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(singleId));

        mockMvc.perform(get("/api/admin/math-questions/{id}", singleId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionType").value("SINGLE"))
            .andExpect(jsonPath("$.data.options[0].key").value("A"))
            .andExpect(jsonPath("$.data.answers[0]").value("B"))
            .andExpect(jsonPath("$.data.questionNo").value(1));

        mockMvc.perform(get("/api/admin/math-questions/{id}", multiId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionType").value("MULTI"))
            .andExpect(jsonPath("$.data.answers.length()").value(2))
            .andExpect(jsonPath("$.data.questionNo").value(2));

        mockMvc.perform(get("/api/admin/math-questions/{id}", fillId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionType").value("FILL"))
            .andExpect(jsonPath("$.data.answers[0]").value("3"))
            .andExpect(jsonPath("$.data.questionNo").value(3));

        mockMvc.perform(get("/api/admin/math-questions/{id}", solutionId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionType").value("SOLUTION"))
            .andExpect(jsonPath("$.data.subQuestions[0].prompt").value("姹?f(2)"))
            .andExpect(jsonPath("$.data.subQuestions[0].steps.length()").value(0));

        assertRuntimeQuestion(singleId, "SINGLE", true, false);
        assertRuntimeQuestion(multiId, "MULTI", true, false);
        assertRuntimeQuestion(fillId, "FILL", false, false);
        assertRuntimeQuestion(solutionId, "SOLUTION", false, true);

        mockMvc.perform(put("/api/admin/math-questions/{id}", solutionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSolutionUpdatePayload(secondSection))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionNo").value(1))
            .andExpect(jsonPath("$.data.subQuestions[0].prompt").value("姹?f(3)"));

        String runtimeQuestionNo = jdbcTemplate.queryForObject(
            "SELECT source_question_no FROM questions WHERE source_math_question_id = ?",
            String.class,
            solutionId
        );
        Assertions.assertEquals("1", runtimeQuestionNo);

        mockMvc.perform(delete("/api/admin/math-questions/{id}", singleId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        String importStatus = jdbcTemplate.queryForObject(
            "SELECT import_status FROM questions WHERE source_math_question_id = ?",
            String.class,
            singleId
        );
        Assertions.assertEquals("REMOVED", importStatus);
    }

    private void assertRuntimeQuestion(long mathQuestionId, String expectedType, boolean expectOptions, boolean expectSubQuestions) {
        Integer runtimeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM questions WHERE source_math_question_id = ? AND import_status = 'READY'",
            Integer.class,
            mathQuestionId
        );
        Assertions.assertEquals(1, runtimeCount);

        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT type, options_json, answer_json, sub_questions_json, import_status, content FROM questions WHERE source_math_question_id = ?",
            mathQuestionId
        );
        Assertions.assertEquals(expectedType, row.get("type"));
        Assertions.assertEquals("READY", row.get("import_status"));
        Assertions.assertEquals(expectOptions, row.get("options_json") != null);
        Assertions.assertEquals(expectSubQuestions, row.get("sub_questions_json") != null);
        Assertions.assertNotNull(row.get("answer_json"));
        Assertions.assertFalse(String.valueOf(row.get("content")).matches("^\\d+\\..*"));
    }

    private Map<String, Object> buildSinglePayload(ChapterPathInfo section, String rawText, int sourceYear, String sourcePaper) {
        Map<String, Object> payload = basePayload(section, rawText, sourceYear, sourcePaper);
        payload.put("questionType", "SINGLE");
        payload.put("options", List.of(
            Map.of("key", "A", "content", "x杞?),
            Map.of("key", "B", "content", "y杞?),
            Map.of("key", "C", "content", "鐩寸嚎 y=x"),
            Map.of("key", "D", "content", "鍘熺偣")
        ));
        payload.put("answers", List.of("B"));
        payload.put("answerLatex", "B");
        payload.put("teacherExplanation", "缁濆鍊煎嚱鏁板浘鍍忓叧浜?y 杞村绉般€?);
        return payload;
    }

    private Map<String, Object> buildMultiPayload(ChapterPathInfo section, String rawText, int sourceYear, String sourcePaper) {
        Map<String, Object> payload = basePayload(section, rawText, sourceYear, sourcePaper);
        payload.put("questionType", "MULTI");
        payload.put("options", List.of(
            Map.of("key", "A", "content", "1"),
            Map.of("key", "B", "content", "2"),
            Map.of("key", "C", "content", "3"),
            Map.of("key", "D", "content", "4")
        ));
        payload.put("answers", List.of("B", "D"));
        payload.put("answerLatex", "B, D");
        payload.put("teacherExplanation", "鏍规嵁鏉′欢姣旇緝鍙緱 B銆丏 姝ｇ‘銆?);
        return payload;
    }

    private Map<String, Object> buildFillPayload(ChapterPathInfo section, String rawText, int sourceYear, String sourcePaper) {
        Map<String, Object> payload = basePayload(section, rawText, sourceYear, sourcePaper);
        payload.put("questionType", "FILL");
        payload.put("answers", List.of("3"));
        payload.put("answerLatex", "3");
        payload.put("teacherExplanation", "鐩存帴浠ｅ叆鍗冲彲寰楀埌 3銆?);
        return payload;
    }

    private Map<String, Object> buildSolutionPayload(ChapterPathInfo section, String rawText, int sourceYear, String sourcePaper) {
        Map<String, Object> payload = basePayload(section, rawText, sourceYear, sourcePaper);
        payload.put("questionType", "SOLUTION");
        payload.put("answers", List.of());
        payload.put("subQuestions", List.of(
            Map.of(
                "index", 1,
                "prompt", "姹?f(2)",
                "referenceAnswer", "4",
                "steps", List.of("浠ｅ叆 x=2", "寰楀埌 f(2)=4")
            ),
            Map.of(
                "index", 2,
                "prompt", "姹?f(3)",
                "referenceAnswer", "9",
                "steps", List.of("浠ｅ叆 x=3", "寰楀埌 f(3)=9")
            )
        ));
        payload.put("answerLatex", "锛?锛?\n锛?锛?");
        payload.put("teacherExplanation", "鍏堜唬鍏ワ紝鍐嶅寲绠€銆?);
        return payload;
    }

    private Map<String, Object> buildSolutionUpdatePayload(ChapterPathInfo section) {
        Map<String, Object> payload = basePayload(section, "宸茬煡 f(x)=x^2锛屾洿鏂板悗鐨勫ぇ棰樸€?, 2026, "妯℃嫙鍗稟");
        payload.put("questionType", "SOLUTION");
        payload.put("answers", List.of());
        payload.put("subQuestions", List.of(
            Map.of(
                "index", 1,
                "prompt", "姹?f(3)",
                "referenceAnswer", "9",
                "steps", List.of("浠ｅ叆 x=3", "寰楀埌 f(3)=9")
            )
        ));
        payload.put("answerLatex", "锛?锛?");
        payload.put("teacherExplanation", "鏇存柊鍚庣殑瑙ｆ瀽銆?);
        return payload;
    }

    private Map<String, Object> basePayload(ChapterPathInfo section, String rawText, int sourceYear, String sourcePaper) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("imageUrl", "/uploads/questions/demo.png");
        payload.put("rawTextLatex", rawText);
        payload.put("bookName", section.bookTitle());
        payload.put("chapterName", section.chapterTitle());
        payload.put("sectionName", section.sectionTitle());
        payload.put("sourceYear", sourceYear);
        payload.put("sourcePaper", sourcePaper);
        return payload;
    }

    private long createQuestion(String token, Map<String, Object> payload) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/admin/math-questions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").isNumber())
            .andReturn();
        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        return createNode.path("data").path("id").asLong();
    }

    private ChapterPathInfo loadChapterPath(Long bookId, Long chapterId, Long sectionId) {
        return jdbcTemplate.queryForObject(
            """
                SELECT b.id AS book_id, c.id AS chapter_id, s.id AS section_id,
                       b.title AS book_title, c.title AS chapter_title, s.title AS section_title
                FROM chapters s
                JOIN chapters c ON s.parent_id = c.id
                JOIN chapters b ON c.parent_id = b.id
                WHERE b.id = ? AND c.id = ? AND s.id = ?
                """,
            (rs, rowNum) -> new ChapterPathInfo(
                rs.getLong("book_id"),
                rs.getLong("chapter_id"),
                rs.getLong("section_id"),
                rs.getString("book_title"),
                rs.getString("chapter_title"),
                rs.getString("section_title")
            ),
            bookId,
            chapterId,
            sectionId
        );
    }

    private ChapterPathInfo loadAlternativeSection(Long excludedSectionId) {
        List<ChapterPathInfo> rows = jdbcTemplate.query(
            """
                SELECT b.id AS book_id, c.id AS chapter_id, s.id AS section_id,
                       b.title AS book_title, c.title AS chapter_title, s.title AS section_title
                FROM chapters s
                JOIN chapters c ON s.parent_id = c.id
                JOIN chapters b ON c.parent_id = b.id
                WHERE s.id <> ?
                ORDER BY b.id ASC, c.id ASC, s.id ASC
                LIMIT 1
                """,
            (rs, rowNum) -> new ChapterPathInfo(
                rs.getLong("book_id"),
                rs.getLong("chapter_id"),
                rs.getLong("section_id"),
                rs.getString("book_title"),
                rs.getString("chapter_title"),
                rs.getString("section_title")
            ),
            excludedSectionId
        );
        Assertions.assertFalse(rows.isEmpty(), "Alternative section is required for filter coverage");
        return rows.get(0);
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

    private record ChapterPathInfo(
        Long bookId,
        Long chapterId,
        Long sectionId,
        String bookTitle,
        String chapterTitle,
        String sectionTitle
    ) {
    }
}
