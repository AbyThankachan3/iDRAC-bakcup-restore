ALTER TABLE backup_host_logs ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();

CREATE INDEX idx_host_status_date ON backup_host_logs(host, status, created_at);