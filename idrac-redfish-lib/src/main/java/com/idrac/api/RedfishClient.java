package com.idrac.api;

import com.idrac.model.*;

public interface RedfishClient {

    String triggerExport(ExportTarget target, ExportFormat exportFormat, ExportUse exportUse, IncludeInExport... includes);

    void pollJob(String jobId, long timeoutMillis) throws InterruptedException;

    String fetchScp(String jobId, ExportFormat exportFormat);

    void cleanupStuckJobs();

    String getSystemModel();
}