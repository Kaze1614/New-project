package com.datong.mathai.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Component
public class SchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Set<String> userColumns = loadColumns("users");
        ensureColumn(userColumns, "users", "role", "VARCHAR(24) NOT NULL DEFAULT 'STUDENT'");
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE LOWER(username) = 'admin'");
        jdbcTemplate.update("UPDATE users SET role = 'STUDENT' WHERE role IS NULL OR role = ''");

        Set<String> reviewTaskColumns = loadColumns("review_tasks");
        ensureColumn(reviewTaskColumns, "review_tasks", "repetition", "INT NOT NULL DEFAULT 0");
        ensureColumn(reviewTaskColumns, "review_tasks", "interval_days", "INT NOT NULL DEFAULT 1");
        ensureColumn(reviewTaskColumns, "review_tasks", "ease_factor", "DECIMAL(4,2) NOT NULL DEFAULT 2.50");
        ensureColumn(reviewTaskColumns, "review_tasks", "suspended", "TINYINT(1) NOT NULL DEFAULT 0");
        ensureColumn(reviewTaskColumns, "review_tasks", "last_grade", "VARCHAR(24) NULL");
        ensureColumn(reviewTaskColumns, "review_tasks", "completed_at", "DATETIME NULL");
    }

    private Set<String> loadColumns(String tableName) throws Exception {
        return jdbcTemplate.execute((ConnectionCallback<Set<String>>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            Set<String> columns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, null)) {
                while (resultSet.next()) {
                    columns.add(resultSet.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            return columns;
        });
    }

    private void ensureColumn(Set<String> existingColumns, String tableName, String columnName, String definition) {
        if (existingColumns.contains(columnName.toLowerCase(Locale.ROOT))) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        existingColumns.add(columnName.toLowerCase(Locale.ROOT));
    }
}
