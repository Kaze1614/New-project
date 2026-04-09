package com.datong.mathai.favorite;

import com.datong.mathai.common.AppException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class FavoriteService {

    private final JdbcTemplate jdbcTemplate;

    public FavoriteService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FavoriteItem> list(Long userId) {
        return jdbcTemplate.query(
            "SELECT id, title, content, created_at FROM favorites WHERE user_id = ? ORDER BY created_at DESC",
            (rs, rowNum) -> new FavoriteItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
            ),
            userId
        );
    }

    public FavoriteItem create(Long userId, CreateFavoriteRequest request) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO favorites(user_id, title, content, created_at) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, request.title());
            ps.setString(3, request.content());
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long id = extractId(keyHolder, "Create favorite failed");
        return new FavoriteItem(id, request.title(), request.content(), now);
    }

    public void delete(Long userId, Long id) {
        int affected = jdbcTemplate.update("DELETE FROM favorites WHERE id = ? AND user_id = ?", id, userId);
        if (affected == 0) {
            throw new AppException(404, "Favorite not found");
        }
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
