package com.datong.mathai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminUserManagementShouldListResetAndDeleteUsers() throws Exception {
        String adminToken = login("admin", "123456");
        String username = "user_" + System.currentTimeMillis();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", "654321"
                ))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode registerNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        long userId = registerNode.path("data").path("user").path("id").asLong();

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .param("keyword", username))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].username").value(username))
            .andExpect(jsonPath("$.data.items[0].role").value("STUDENT"));

        mockMvc.perform(post("/api/admin/users/{id}/reset-password", userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String hash = jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, userId);
        assert hash != null;
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("123456", hash));

        mockMvc.perform(delete("/api/admin/users/{id}", userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users WHERE id = ?", Integer.class, userId);
        org.junit.jupiter.api.Assertions.assertEquals(0, count);

        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        mockMvc.perform(delete("/api/admin/users/{id}", adminId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("admin 账号不可删除"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", password
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
    }
}
