package com.idrac.restore.client;

import com.idrac.restore.model.JobResponse;
import com.idrac.restore.model.RestorePlan;
import com.idrac.restore.model.RestoreTaskResult;

public interface RestoreClient {

    JobResponse preview(String xmlContent);
    JobResponse importConfig(String xmlContent);
    JobResponse getJob(String jobId);
    void pollJob(String jobId, int timeoutSeconds) throws InterruptedException;
    void pollJob(String jobId, int timeoutSeconds, JobProgressListener listener) throws InterruptedException;
    RestorePlan analyseXml(String xmlContent, String currentHost);
    RestoreTaskResult fetchTaskResult(String jobId);
}
