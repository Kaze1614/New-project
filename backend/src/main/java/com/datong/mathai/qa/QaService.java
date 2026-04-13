package com.datong.mathai.qa;

import com.datong.mathai.ai.AIProvider;
import com.datong.mathai.ai.ChatReplyPayload;
import com.datong.mathai.common.AppException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class QaService {

    private final JdbcTemplate jdbcTemplate;
    private final AIProvider aiProvider;

    public QaService(JdbcTemplate jdbcTemplate, AIProvider aiProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiProvider = aiProvider;
    }

    public QaSessionItem createSession(Long userId, CreateSessionRequest request) {
        String title = request.title() == null || request.title().isBlank() ? "New Session" : request.title().trim();
        LocalDateTime now = LocalDateTime.now();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO qa_sessions(user_id, title, created_at, updated_at) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setTimestamp(4, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long id = extractId(keyHolder, "Create QA session failed");
        return new QaSessionItem(id, title, now, now);
    }

    public List<QaSessionItem> listSessions(Long userId) {
        return jdbcTemplate.query(
            "SELECT id, title, created_at, updated_at FROM qa_sessions WHERE user_id = ? ORDER BY updated_at DESC",
            (rs, rowNum) -> new QaSessionItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
            ),
            userId
        );
    }

    public List<QaMessageItem> listMessages(Long userId, Long sessionId) {
        ensureSessionOwner(userId, sessionId);
        return jdbcTemplate.query(
            "SELECT id, session_id, role, content, created_at FROM qa_messages WHERE session_id = ? ORDER BY created_at ASC",
            (rs, rowNum) -> new QaMessageItem(
                rs.getLong("id"),
                rs.getLong("session_id"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
            ),
            sessionId
        );
    }

    public QaConversationTurn sendMessage(Long userId, Long sessionId, SendMessageRequest request) {
        ensureSessionOwner(userId, sessionId);

        LocalDateTime now = LocalDateTime.now();
        Long userMsgId = insertMessage(sessionId, "user", request.content(), now);
        QaMessageItem userMsg = new QaMessageItem(userMsgId, sessionId, "user", request.content(), now);

        List<String> context = jdbcTemplate.query(
            "SELECT role, content FROM qa_messages WHERE session_id = ? ORDER BY created_at DESC LIMIT 10",
            (rs, rowNum) -> rs.getString("role") + ": " + rs.getString("content"),
            sessionId
        );
        if (request.context() != null && !request.context().isBlank()) {
            context.add(0, "context: " + request.context().trim());
        }

        ChatReplyPayload reply = aiProvider.reply(request.content(), context);
        LocalDateTime assistantNow = LocalDateTime.now();
        Long assistantMsgId = insertMessage(sessionId, "assistant", reply.answer(), assistantNow);
        QaMessageItem assistantMsg = new QaMessageItem(assistantMsgId, sessionId, "assistant", reply.answer(), assistantNow);

        jdbcTemplate.update("UPDATE qa_sessions SET updated_at = NOW() WHERE id = ?", sessionId);
        return new QaConversationTurn(userMsg, assistantMsg);
    }

    private void ensureSessionOwner(Long userId, Long sessionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM qa_sessions WHERE id = ? AND user_id = ?",
            Integer.class,
            sessionId,
            userId
        );
        if (count == null || count == 0) {
            throw new AppException(404, "Session not found");
        }
    }

    private Long insertMessage(Long sessionId, String role, String content, LocalDateTime createdAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO qa_messages(session_id, role, content, created_at) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, sessionId);
            ps.setString(2, role);
            ps.setString(3, content);
            ps.setTimestamp(4, Timestamp.valueOf(createdAt));
            return ps;
        }, keyHolder);
        return extractId(keyHolder, "Create QA message failed");
    }

    private Long extractId(KeyHolder keyHolder, String errorMessage) {
        try {
            Number key = keyHolder.getKey();
            if (key != null) {
                return key.longValue();
            }
        } catch (Exception ignored) {
        }
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            Object idValue = keys.get("id");
            if (idValue instanceof Number number) {
                return number.longValue();
            }
            Object firstValue = keys.values().stream().findFirst().orElse(null);
            if (firstValue instanceof Number number) {
                return number.longValue();
            }
        }
        throw new AppException(500, errorMessage);
    }
}
