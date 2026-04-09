package com.datong.mathai.chapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChapterService {

    private final JdbcTemplate jdbcTemplate;

    public ChapterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ChapterNode> tree() {
        List<ChapterRow> rows = jdbcTemplate.query(
            "SELECT id, parent_id, title, sort_order FROM chapters ORDER BY sort_order ASC, id ASC",
            (rs, rowNum) -> new ChapterRow(
                rs.getLong("id"),
                rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                rs.getString("title"),
                rs.getInt("sort_order")
            )
        );

        Map<Long, ChapterNode> nodeMap = new LinkedHashMap<>();
        rows.forEach(row -> nodeMap.put(row.id(), ChapterNode.of(row.id(), row.title(), row.sortOrder())));

        List<ChapterNode> roots = new ArrayList<>();
        for (ChapterRow row : rows) {
            ChapterNode node = nodeMap.get(row.id());
            if (row.parentId() == null) {
                roots.add(node);
            } else {
                ChapterNode parent = nodeMap.get(row.parentId());
                if (parent != null) {
                    parent.children().add(node);
                }
            }
        }
        return roots;
    }

    private record ChapterRow(Long id, Long parentId, String title, Integer sortOrder) {
    }
}
