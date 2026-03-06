package com.idrac.backup.transport;

import com.idrac.backup.model.ExportRequest;
import com.idrac.backup.model.JobResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;

public interface FeignRedfishApi {

    @RequestLine("POST /redfish/v1/Managers/iDRAC.Embedded.1/Actions/Oem/EID_674_Manager.ExportSystemConfiguration")
    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    Response triggerExport(ExportRequest payload);

    @RequestLine("GET /redfish/v1/Managers/iDRAC.Embedded.1/Oem/Dell/Jobs/{jobId}")
    @Headers("Accept: application/json")
    JobResponse getJob(@Param("jobId") String jobId);

    @RequestLine("GET /redfish/v1/TaskService/Tasks/{jobId}")
    @Headers("Accept: application/json")
    Response getTask(@Param("jobId") String jobId);

    @RequestLine("GET /redfish/v1/Managers/iDRAC.Embedded.1/Oem/Dell/Jobs")
    @Headers("Accept: application/json")
    JobResponse getJobs();

    @RequestLine("DELETE /redfish/v1/Managers/iDRAC.Embedded.1/Oem/Dell/Jobs/{jobId}")
    void deleteJob(@Param("jobId") String jobId);

    @RequestLine("GET /redfish/v1/Systems/System.Embedded.1")
    @Headers("Accept: application/json")
    Response getSystem();
}