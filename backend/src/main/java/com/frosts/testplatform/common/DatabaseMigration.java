package com.frosts.testplatform.common;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional
    public void migrate() {
        log.info("Running database migration...");

        // Fix sys_role table
        addColumnIfNotExists("sys_role", "type", "VARCHAR(20)");
        addColumnIfNotExists("sys_role", "enabled", "BOOLEAN DEFAULT TRUE");
        addColumnIfNotExists("sys_role", "is_system", "BOOLEAN DEFAULT FALSE");
        jdbcTemplate.execute("UPDATE sys_role SET enabled = TRUE WHERE enabled IS NULL");
        jdbcTemplate.execute("UPDATE sys_role SET is_system = FALSE WHERE is_system IS NULL");

        // Fix sys_permission table
        addColumnIfNotExists("sys_permission", "category", "VARCHAR(50)");
        addColumnIfNotExists("sys_permission", "parent_id", "BIGINT");
        addColumnIfNotExists("sys_permission", "sort_order", "INTEGER DEFAULT 0");
        addColumnIfNotExists("sys_permission", "icon", "VARCHAR(50)");
        addColumnIfNotExists("sys_permission", "menu_url", "VARCHAR(200)");
        addColumnIfNotExists("sys_permission", "perm_type", "VARCHAR(20)");

        // Fix sys_user table
        addColumnIfNotExists("sys_user", "avatar", "VARCHAR(200)");
        addColumnIfNotExists("sys_user", "department", "VARCHAR(100)");
        addColumnIfNotExists("sys_user", "position", "VARCHAR(100)");
        addColumnIfNotExists("sys_user", "last_login_at", "TIMESTAMP");
        addColumnIfNotExists("sys_user", "last_login_ip", "VARCHAR(50)");
        addColumnIfNotExists("sys_user", "login_count", "INTEGER DEFAULT 0");
        addColumnIfNotExists("sys_user", "account_non_locked", "BOOLEAN DEFAULT TRUE");
        addColumnIfNotExists("sys_user", "lock_reason", "VARCHAR(200)");

        // Fix project table
        addColumnIfNotExists("project", "actual_end_date", "DATE");
        addColumnIfNotExists("project", "category", "VARCHAR(30)");
        addColumnIfNotExists("project", "progress", "DECIMAL(5,2)");
        addColumnIfNotExists("project", "estimated_hours", "INTEGER");
        addColumnIfNotExists("project", "actual_hours", "INTEGER");
        addColumnIfNotExists("project", "health", "VARCHAR(20)");

        // Fix requirement table
        addColumnIfNotExists("requirement", "acceptance_criteria", "TEXT");
        addColumnIfNotExists("requirement", "story_points", "INTEGER");
        addColumnIfNotExists("requirement", "estimated_hours", "INTEGER");
        addColumnIfNotExists("requirement", "actual_hours", "INTEGER");
        addColumnIfNotExists("requirement", "due_date", "DATE");
        addColumnIfNotExists("requirement", "completed_date", "DATE");
        addColumnIfNotExists("requirement", "source", "VARCHAR(50)");
        addColumnIfNotExists("requirement", "rejected_reason", "TEXT");

        // Fix test_case table
        addColumnIfNotExists("test_case", "test_data", "TEXT");
        addColumnIfNotExists("test_case", "postconditions", "TEXT");
        addColumnIfNotExists("test_case", "execution_time", "INTEGER");
        addColumnIfNotExists("test_case", "is_automated", "BOOLEAN DEFAULT FALSE");
        addColumnIfNotExists("test_case", "automation_script", "VARCHAR(500)");
        addColumnIfNotExists("test_case", "reviewer", "VARCHAR(50)");
        addColumnIfNotExists("test_case", "review_status", "VARCHAR(20)");
        addColumnIfNotExists("test_case", "review_comments", "TEXT");
        addColumnIfNotExists("test_case", "tags", "VARCHAR(200)");
        addColumnIfNotExists("test_case", "version", "VARCHAR(20) DEFAULT '1.0'");
        addColumnIfNotExists("test_case", "last_executed_at", "TIMESTAMP");
        addColumnIfNotExists("test_case", "last_executed_by", "VARCHAR(50)");
        addColumnIfNotExists("test_case", "pass_count", "INTEGER DEFAULT 0");
        addColumnIfNotExists("test_case", "fail_count", "INTEGER DEFAULT 0");
        addColumnIfNotExists("test_case", "total_executions", "INTEGER DEFAULT 0");

        // Migrate old automated column if exists
        if (columnExists("test_case", "automated")) {
            jdbcTemplate.execute("UPDATE test_case SET is_automated = TRUE WHERE automated IS NOT NULL AND automated != ''");
            jdbcTemplate.execute("ALTER TABLE test_case DROP COLUMN automated");
        }

        // Fix test_plan table
        addColumnIfNotExists("test_plan", "plan_number", "VARCHAR(50)");
        addColumnIfNotExists("test_plan", "actual_start_date", "DATE");
        addColumnIfNotExists("test_plan", "actual_end_date", "DATE");
        addColumnIfNotExists("test_plan", "environment", "VARCHAR(100)");
        addColumnIfNotExists("test_plan", "milestone", "VARCHAR(100)");
        addColumnIfNotExists("test_plan", "progress", "DECIMAL(5,2)");
        addColumnIfNotExists("test_plan", "total_cases", "INTEGER DEFAULT 0");
        addColumnIfNotExists("test_plan", "passed_cases", "INTEGER DEFAULT 0");
        addColumnIfNotExists("test_plan", "failed_cases", "INTEGER DEFAULT 0");
        addColumnIfNotExists("test_plan", "blocked_cases", "INTEGER DEFAULT 0");
        addColumnIfNotExists("test_plan", "not_run_cases", "INTEGER DEFAULT 0");
        addColumnIfNotExists("test_plan", "risk", "TEXT");
        addColumnIfNotExists("test_plan", "entry_criteria", "TEXT");
        addColumnIfNotExists("test_plan", "exit_criteria", "TEXT");

        // Fix test_plan_case table
        addColumnIfNotExists("test_plan_case", "defect_id", "VARCHAR(50)");
        addColumnIfNotExists("test_plan_case", "defect_link", "VARCHAR(500)");
        addColumnIfNotExists("test_plan_case", "evidence", "TEXT");
        addColumnIfNotExists("test_plan_case", "retry_count", "INTEGER DEFAULT 0");
        addColumnIfNotExists("test_plan_case", "is_blocked", "BOOLEAN DEFAULT FALSE");
        addColumnIfNotExists("test_plan_case", "block_reason", "TEXT");

        // Fix defect table
        addColumnIfNotExists("defect", "verified_by", "VARCHAR(50)");
        addColumnIfNotExists("defect", "verified_at", "TIMESTAMP");
        addColumnIfNotExists("defect", "found_in_version", "VARCHAR(50)");
        addColumnIfNotExists("defect", "fixed_in_version", "VARCHAR(50)");
        addColumnIfNotExists("defect", "component", "VARCHAR(100)");
        addColumnIfNotExists("defect", "reproducibility", "VARCHAR(30)");
        addColumnIfNotExists("defect", "impact", "TEXT");
        addColumnIfNotExists("defect", "workaround", "TEXT");
        addColumnIfNotExists("defect", "root_cause", "TEXT");
        addColumnIfNotExists("defect", "duplicate_of", "BIGINT");
        addColumnIfNotExists("defect", "reopen_count", "INTEGER DEFAULT 0");
        addColumnIfNotExists("defect", "source", "VARCHAR(50)");

        log.info("Database migration completed successfully.");
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
        if (!columnExists(tableName, columnName)) {
            String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition);
            jdbcTemplate.execute(sql);
            log.info("Added column {} to table {}", columnName, tableName);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
