package com.idrac.restore.client;

import com.idrac.restore.model.JobResponse;

public interface JobProgressListener {

    void onUpdate(JobResponse job);

}
