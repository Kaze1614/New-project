package com.datong.mathai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChapterTreeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chapterTreeShouldUseFullHighSchoolMathCatalog() throws Exception {
        mockMvc.perform(get("/api/chapters/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.data[0].title").value("必修第一册"))
            .andExpect(jsonPath("$.data[0].children[0].title").value("第一章 集合与常用逻辑用语"))
            .andExpect(jsonPath("$.data[0].children[0].children[0].title").value("集合的概念"))
            .andExpect(jsonPath("$.data[4].title").value("选择性必修第三册"))
            .andExpect(jsonPath("$.data[4].children[2].title").value("第八章 成对数据的统计分析"))
            .andExpect(jsonPath("$.data[4].children[2].children[2].title").value("列联表与独立性检验"));
    }
}