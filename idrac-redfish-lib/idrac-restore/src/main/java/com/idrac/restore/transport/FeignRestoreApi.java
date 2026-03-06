package com.idrac.restore.transport;

import com.idrac.restore.model.JobResponse;
import com.idrac.restore.model.RestoreRequest;
import com.idrac.restore.model.TaskResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;

import java.net.URI;

public interface FeignRestoreApi {

    @RequestLine("POST /redfish/v1/Managers/iDRAC.Embedded.1/Actions/Oem/EID_674_Manager.ImportSystemConfigurationPreview")
    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    Response preview(RestoreRequest request);

    @RequestLine("POST /redfish/v1/Managers/iDRAC.Embedded.1/Actions/Oem/EID_674_Manager.ImportSystemConfiguration")
    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    Response importConfiguration(RestoreRequest request);

    @RequestLine("GET /redfish/v1/Managers/iDRAC.Embedded.1/Oem/Dell/Jobs/{jobId}")
    @Headers("Accept: application/json")
    JobResponse getJob(@Param("jobId") String jobId);

    @RequestLine("GET /redfish/v1/TaskService/Tasks/{jobId}")
    @Headers("Accept: application/json")
    TaskResponse getTask(@Param("jobId") String jobId);
}
