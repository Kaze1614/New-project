package com.datong.mathai.favorite;

import com.datong.mathai.common.AppException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FavoriteService {

    private final JdbcTemplate jdbcTemplate;

    public FavoriteService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FavoriteItem> list(Long userId, Long chapterId, String difficulty, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT id, chapter_id, difficulty, title, content, created_at FROM favorites WHERE user_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);

        if (chapterId != null) {
            sql.append(" AND chapter_id = ?");
            args.add(chapterId);
        }
        if (difficulty != null && !difficulty.isBlank()) {
            sql.append(" AND difficulty = ?");
            args.add(difficulty.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (title LIKE ? OR content LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY created_at DESC");

        return jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> new FavoriteItem(
                rs.getLong("id"),
                rs.getObject("chapter_id") == null ? null : rs.getLong("chapter_id"),
                rs.getString("difficulty"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
            ),
            args.toArray()
        );
    }

    public FavoriteItem create(Long userId, CreateFavoriteRequest request) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO favorites(user_id, chapter_id, difficulty, title, content, created_at) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            if (request.chapterId() == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, request.chapterId());
            }
            ps.setString(3, request.difficulty() == null ? null : request.difficulty().trim().toUpperCase());
            ps.setString(4, request.title());
            ps.setString(5, request.content());
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long id = extractId(keyHolder, "Create favorite failed");
        return new FavoriteItem(id, request.chapterId(), request.difficulty(), request.title(), request.content(), now);
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
