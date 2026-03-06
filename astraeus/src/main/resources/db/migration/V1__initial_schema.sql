
-- Table: idrac_servers

CREATE TABLE idrac_servers (
   id BIGSERIAL PRIMARY KEY,
   host VARCHAR(255) NOT NULL,
   model VARCHAR(255) NOT NULL,
   vault_path VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX idx_idrac_servers_host ON idrac_servers(host);


-- Table: backup_jobs

CREATE TABLE backup_jobs (
     id BIGSERIAL PRIMARY KEY,
     started_at TIMESTAMP,
     finished_at TIMESTAMP,
     total_servers INTEGER,
     success_count INTEGER,
     failure_count INTEGER,
     status VARCHAR(50)
);

CREATE INDEX idx_backup_jobs_status ON backup_jobs(status);

-- Table: backup_host_logs

CREATE TABLE backup_host_logs (
      id BIGSERIAL PRIMARY KEY,
      job_id BIGINT NOT NULL,
      created_at TIMESTAMP NOT NULL,
      host VARCHAR(255),
      status VARCHAR(50),
      redfish_job_id VARCHAR(100),
      percent INTEGER,
      file_path VARCHAR(500),
      error_message VARCHAR(2000),
      duration_millis BIGINT,

      CONSTRAINT fk_backup_host_logs_job FOREIGN KEY (job_id) REFERENCES backup_jobs(id) ON DELETE CASCADE
);

CREATE INDEX idx_backup_host_logs_job ON backup_host_logs(job_id);
CREATE INDEX idx_backup_host_logs_host ON backup_host_logs(host);
CREATE INDEX idx_backup_host_logs_status ON backup_host_logs(status);


-- Table: bulk_register_jobs
CREATE TABLE bulk_register_jobs (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    total INTEGER,
    success_count INTEGER,
    failure_count INTEGER,
    status VARCHAR(50)
);

CREATE INDEX idx_bulk_register_jobs_status ON bulk_register_jobs(status);

-- Table: bulk_register_failures
CREATE TABLE bulk_register_failures (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    host VARCHAR(255),
    error VARCHAR(2000),

    CONSTRAINT fk_bulk_register_failures_job FOREIGN KEY (job_id) REFERENCES bulk_register_jobs(id) ON DELETE CASCADE
);

CREATE INDEX idx_bulk_register_failures_job ON bulk_register_failures(job_id);
CREATE INDEX idx_bulk_register_failures_host ON bulk_register_failures(host);

-- Table: restore_log
CREATE TABLE restore_log (
     id BIGSERIAL PRIMARY KEY,
     backup_job_id BIGINT,
     redfish_job_id VARCHAR(100),
     initial_host VARCHAR(255),
     final_host VARCHAR(255),
     status VARCHAR(50),
     percent INTEGER,
     reboot_required BOOLEAN,
     failures JSONB,
     warnings JSONB,
     started_at TIMESTAMP,
     completed_at TIMESTAMP
);

CREATE INDEX idx_restore_log_backup_job ON restore_log(backup_job_id);
CREATE INDEX idx_restore_log_status ON restore_log(status);
CREATE INDEX idx_restore_log_redfish_job ON restore_log(redfish_job_id);