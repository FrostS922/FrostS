-- Fix sys_role table: add new columns and set defaults
ALTER TABLE sys_role
    ADD COLUMN IF NOT EXISTS type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS is_system BOOLEAN DEFAULT FALSE;

-- Update existing rows to have default values
UPDATE sys_role SET enabled = TRUE WHERE enabled IS NULL;
UPDATE sys_role SET is_system = FALSE WHERE is_system IS NULL;

-- Fix sys_permission table: add new columns
ALTER TABLE sys_permission
    ADD COLUMN IF NOT EXISTS category VARCHAR(50),
    ADD COLUMN IF NOT EXISTS parent_id BIGINT,
    ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS icon VARCHAR(50),
    ADD COLUMN IF NOT EXISTS menu_url VARCHAR(200),
    ADD COLUMN IF NOT EXISTS perm_type VARCHAR(20);

-- Fix sys_user table: add new columns
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS avatar VARCHAR(200),
    ADD COLUMN IF NOT EXISTS department VARCHAR(100),
    ADD COLUMN IF NOT EXISTS position VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(50),
    ADD COLUMN IF NOT EXISTS login_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS account_non_locked BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS lock_reason VARCHAR(200);

-- Fix project table: add new columns
ALTER TABLE project
    ADD COLUMN IF NOT EXISTS actual_end_date DATE,
    ADD COLUMN IF NOT EXISTS category VARCHAR(30),
    ADD COLUMN IF NOT EXISTS progress DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS estimated_hours INTEGER,
    ADD COLUMN IF NOT EXISTS actual_hours INTEGER,
    ADD COLUMN IF NOT EXISTS health VARCHAR(20);

-- Fix requirement table: add new columns
ALTER TABLE requirement
    ADD COLUMN IF NOT EXISTS acceptance_criteria TEXT,
    ADD COLUMN IF NOT EXISTS story_points INTEGER,
    ADD COLUMN IF NOT EXISTS estimated_hours INTEGER,
    ADD COLUMN IF NOT EXISTS actual_hours INTEGER,
    ADD COLUMN IF NOT EXISTS due_date DATE,
    ADD COLUMN IF NOT EXISTS completed_date DATE,
    ADD COLUMN IF NOT EXISTS source VARCHAR(50),
    ADD COLUMN IF NOT EXISTS rejected_reason TEXT;

-- Fix test_case table: add new columns and rename automated
ALTER TABLE test_case
    ADD COLUMN IF NOT EXISTS test_data TEXT,
    ADD COLUMN IF NOT EXISTS postconditions TEXT,
    ADD COLUMN IF NOT EXISTS execution_time INTEGER,
    ADD COLUMN IF NOT EXISTS is_automated BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS automation_script VARCHAR(500),
    ADD COLUMN IF NOT EXISTS reviewer VARCHAR(50),
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS review_comments TEXT,
    ADD COLUMN IF NOT EXISTS tags VARCHAR(200),
    ADD COLUMN IF NOT EXISTS version VARCHAR(20) DEFAULT '1.0',
    ADD COLUMN IF NOT EXISTS last_executed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_executed_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS pass_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fail_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_executions INTEGER DEFAULT 0;

-- Migrate old automated column to is_automated if exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'test_case' AND column_name = 'automated') THEN
        UPDATE test_case SET is_automated = TRUE WHERE automated IS NOT NULL AND automated != '';
        ALTER TABLE test_case DROP COLUMN automated;
    END IF;
END $$;

-- Fix test_plan table: add new columns
ALTER TABLE test_plan
    ADD COLUMN IF NOT EXISTS plan_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS actual_start_date DATE,
    ADD COLUMN IF NOT EXISTS actual_end_date DATE,
    ADD COLUMN IF NOT EXISTS environment VARCHAR(100),
    ADD COLUMN IF NOT EXISTS milestone VARCHAR(100),
    ADD COLUMN IF NOT EXISTS progress DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS total_cases INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS passed_cases INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failed_cases INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS blocked_cases INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS not_run_cases INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS risk TEXT,
    ADD COLUMN IF NOT EXISTS entry_criteria TEXT,
    ADD COLUMN IF NOT EXISTS exit_criteria TEXT;

-- Fix test_plan_case table: add new columns
ALTER TABLE test_plan_case
    ADD COLUMN IF NOT EXISTS defect_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS defect_link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS evidence TEXT,
    ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS block_reason TEXT;

-- Fix defect table: add new columns
ALTER TABLE defect
    ADD COLUMN IF NOT EXISTS verified_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS found_in_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS fixed_in_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS component VARCHAR(100),
    ADD COLUMN IF NOT EXISTS reproducibility VARCHAR(30),
    ADD COLUMN IF NOT EXISTS impact TEXT,
    ADD COLUMN IF NOT EXISTS workaround TEXT,
    ADD COLUMN IF NOT EXISTS root_cause TEXT,
    ADD COLUMN IF NOT EXISTS duplicate_of BIGINT,
    ADD COLUMN IF NOT EXISTS reopen_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS source VARCHAR(50);
