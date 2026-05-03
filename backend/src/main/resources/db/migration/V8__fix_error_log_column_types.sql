ALTER TABLE sys_error_log ALTER COLUMN error_message TYPE TEXT USING error_message::text;
ALTER TABLE sys_error_log ALTER COLUMN stack_trace TYPE TEXT USING stack_trace::text;
ALTER TABLE sys_error_log ALTER COLUMN extra_info TYPE TEXT USING extra_info::text;
ALTER TABLE sys_error_log ALTER COLUMN page_url TYPE TEXT USING page_url::text;
ALTER TABLE sys_error_log ALTER COLUMN user_agent TYPE TEXT USING user_agent::text;
ALTER TABLE sys_error_log ALTER COLUMN fallback_message TYPE TEXT USING fallback_message::text;
ALTER TABLE sys_error_log ALTER COLUMN category TYPE VARCHAR(50) USING category::varchar;
