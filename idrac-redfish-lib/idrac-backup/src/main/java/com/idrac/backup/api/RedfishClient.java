package com.idrac.backup.api;

import com.idrac.backup.model.*;

public interface RedfishClient {

    String triggerExport(ExportTarget target, ExportFormat exportFormat, ExportUse exportUse, IncludeInExport... includes);
    void pollJob(String jobId, long timeoutMillis) throws InterruptedException;
    String fetchScp(String jobId, ExportFormat exportFormat);
    String getSystemModel();
    void pollJob(String jobId, int timeoutSeconds, JobProgressListener listener) throws InterruptedException;
    JobResponse getJob(String jobId);
}