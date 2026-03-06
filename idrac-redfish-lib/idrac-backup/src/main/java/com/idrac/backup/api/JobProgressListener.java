package com.idrac.backup.api;

import com.idrac.backup.model.JobResponse;

public interface JobProgressListener {

    void onUpdate(JobResponse job);

}
