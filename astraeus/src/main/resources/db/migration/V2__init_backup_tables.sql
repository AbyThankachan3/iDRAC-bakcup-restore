CREATE TABLE backup_jobs (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    total_servers INT,
    success_count INT,
    failure_count INT,
    status VARCHAR(50)
);

CREATE TABLE backup_host_logs (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    host VARCHAR(255),
    status VARCHAR(50),
    file_path TEXT,
    error_message TEXT,
    duration_millis BIGINT,
    CONSTRAINT fk_job FOREIGN KEY (job_id)
    REFERENCES backup_jobs(id)
    ON DELETE CASCADE
);