package com.datong.mathai;

import com.datong.mathai.config.SchemaMigrationRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SchemaMigrationRunnerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaMigrationRunner schemaMigrationRunner;

    @Test
    void runnerShouldRestoreMissingColumnsForLegacyTables() throws Exception {
        jdbcTemplate.update("DELETE FROM questions");
        jdbcTemplate.update(
            """
                INSERT INTO questions(
                    chapter_id, title, content, type, answer_json, explanation, difficulty, import_status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
            4L,
            "Legacy runtime seed",
            "Legacy runtime seed content",
            "FILL",
            "[\"42\"]",
            "Legacy explanation",
            "MEDIUM",
            "READY"
        );

        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_mistake_question");
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_favorite_question");
        jdbcTemplate.execute("ALTER TABLE mistake_records DROP COLUMN question_id");
        jdbcTemplate.execute("ALTER TABLE mistake_records DROP COLUMN difficulty");
        jdbcTemplate.execute("ALTER TABLE favorites DROP COLUMN question_id");
        jdbcTemplate.execute("ALTER TABLE favorites DROP COLUMN chapter_id");
        jdbcTemplate.execute("ALTER TABLE favorites DROP COLUMN difficulty");
        jdbcTemplate.execute("ALTER TABLE questions DROP COLUMN sub_questions_json");
        jdbcTemplate.execute("ALTER TABLE math_questions DROP COLUMN question_type");
        jdbcTemplate.execute("ALTER TABLE math_questions DROP COLUMN options_json");
        jdbcTemplate.execute("ALTER TABLE math_questions DROP COLUMN answer_json");
        jdbcTemplate.execute("ALTER TABLE math_questions DROP COLUMN sub_questions_json");

        schemaMigrationRunner.run(new DefaultApplicationArguments(new String[0]));

        assertTrue(hasColumn("mistake_records", "question_id"));
        assertTrue(hasColumn("mistake_records", "difficulty"));
        assertTrue(hasColumn("favorites", "question_id"));
        assertTrue(hasColumn("favorites", "chapter_id"));
        assertTrue(hasColumn("favorites", "difficulty"));
        assertTrue(hasColumn("questions", "sub_questions_json"));
        assertTrue(hasColumn("math_questions", "question_type"));
        assertTrue(hasColumn("math_questions", "options_json"));
        assertTrue(hasColumn("math_questions", "answer_json"));
        assertTrue(hasColumn("math_questions", "sub_questions_json"));
        assertEquals(
            "LEGACY_SEED",
            jdbcTemplate.queryForObject(
                "SELECT import_status FROM questions WHERE title = 'Legacy runtime seed'",
                String.class
            )
        );

        jdbcTemplate.queryForList("SELECT question_id, difficulty FROM mistake_records WHERE 1 = 0");
        jdbcTemplate.queryForList("SELECT question_id, chapter_id, difficulty FROM favorites WHERE 1 = 0");
        jdbcTemplate.queryForList("SELECT sub_questions_json FROM questions WHERE 1 = 0");
        jdbcTemplate.queryForList("SELECT question_type, options_json, answer_json, sub_questions_json FROM math_questions WHERE 1 = 0");
    }

    private boolean hasColumn(String tableName, String columnName) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (ResultSet resultSet = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
                if (resultSet.next()) {
                    return true;
                }
            }
            try (ResultSet resultSet = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
                return resultSet.next();
            }
        }));
    }
}
