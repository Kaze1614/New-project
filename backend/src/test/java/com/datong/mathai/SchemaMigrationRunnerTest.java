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
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_mistake_question");
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_favorite_question");
        jdbcTemplate.execute("ALTER TABLE mistake_records DROP COLUMN question_id");
        jdbcTemplate.execute("ALTER TABLE mistake_records DROP COLUMN difficulty");
        jdbcTemplate.execute("ALTER TABLE favorites DROP COLUMN question_id");

        schemaMigrationRunner.run(new DefaultApplicationArguments(new String[0]));

        assertTrue(hasColumn("mistake_records", "question_id"));
        assertTrue(hasColumn("mistake_records", "difficulty"));
        assertTrue(hasColumn("favorites", "question_id"));

        jdbcTemplate.queryForList("SELECT question_id, difficulty FROM mistake_records WHERE 1 = 0");
        jdbcTemplate.queryForList("SELECT question_id FROM favorites WHERE 1 = 0");
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
