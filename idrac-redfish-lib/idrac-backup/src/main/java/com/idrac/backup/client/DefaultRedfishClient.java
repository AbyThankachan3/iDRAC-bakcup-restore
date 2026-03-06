package com.idrac.backup.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idrac.backup.api.JobProgressListener;
import com.idrac.backup.api.RedfishClient;
import com.idrac.backup.model.*;
import com.idrac.backup.transport.FeignRedfishApi;
import feign.Response;
import lombok.RequiredArgsConstructor;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

@RequiredArgsConstructor
public class DefaultRedfishClient implements RedfishClient {

    private final FeignRedfishApi feignApi;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String triggerExport(ExportTarget target, ExportFormat exportFormat, ExportUse exportUse, IncludeInExport... includes) {
        try {
            System.out.println("[*] Triggering SCP export (Target=" + target + ", Format=" + exportFormat + ") ...");
            String includeInExport = String.join(",", Arrays.stream(includes)
                                    .map(IncludeInExport::getValue)
                                    .toArray(String[]::new));
            ExportRequest request = ExportRequest.builder()
                            .exportFormat(exportFormat)
                            .exportUse(exportUse)
                            .includeInExport(includeInExport)
                            .shareParameters(
                                    ShareParameters.builder()
                                            .target(target)
                                            .fileName("idrac-config")
                                            .build()
                            )
                            .build();

            Response response = feignApi.triggerExport(request);
            int status = response.status();

            // Read response body ONCE safely
            String body = null;
            if (response.body() != null) {
                try (InputStream is = response.body().asInputStream()) {
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            if (status != 200 && status != 202) {
                throw new RuntimeException("Export request failed (" + status + "): " + body);
            }

            // Extract Location header
            String location = null;
            if (response.headers().containsKey("Location")) {
                location = response.headers().get("Location").iterator().next();
            }

            System.out.println("Location header: " + location);
            // fallback to body @odata.id
            if ((location == null || location.isEmpty()) && body != null && body.contains("@odata.id")) {
                JsonNode json = mapper.readTree(body);
                location = json.path("@odata.id").asText(null);
            }
            if (location == null || location.isEmpty()) {
                throw new RuntimeException("No job Location returned in response.");
            }

            String jobId = location.substring(location.lastIndexOf("/") + 1);

            System.out.println("[+] Export job created: " + jobId);
            return jobId;

        }
        catch (Exception e) {
            throw new RuntimeException("Failed to trigger export: " + e.getMessage(), e);
        }
    }

    @Override
    public void pollJob(String jobId, long timeoutMillis)
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + timeoutMillis;
        int attempt = 0;
        System.out.println("[*] Polling job " + jobId + " ...");
        while (System.currentTimeMillis() < deadline) {
            attempt++;
            Thread.sleep(5000);
            try {
                JobResponse job = feignApi.getJob(jobId);;
                if (job == null) {
                    throw new RuntimeException(
                            "Empty job response from iDRAC"
                    );
                }
                String state = job.getJobState();
                Integer percent = job.getPercentComplete();
                String message = job.getMessage();
                String percentStr = percent != null ? percent.toString() : "?";
                String messageShort = message != null && message.length() > 60 ? message.substring(0, 60) : message;
                System.out.printf("    [%03d] %-30s %s%%  %s%n", attempt, state, percentStr, messageShort != null ? messageShort : "");

                // SUCCESS
                if ("Completed".equalsIgnoreCase(state) || "CompletedWithErrors".equalsIgnoreCase(state)) {
                    System.out.println("[+] Job finished: " + state);
                    return;
                }

                // FAILURE
                if ("Failed".equalsIgnoreCase(state) || "Exception".equalsIgnoreCase(state) || "Cancelled".equalsIgnoreCase(state)) {
                    throw new RuntimeException("Job failed: " + state + " — " + message);
                }

            } catch (Exception e) {
                System.out.printf("    [%03d] Polling error: %s%n", attempt, e.getMessage());
            }
        }

        throw new RuntimeException("[!] Timed out after " + (timeoutMillis / 1000) + " seconds.");
    }

    @Override
    public String fetchScp(String jobId, ExportFormat exportFormat) {

        try {System.out.println("[*] Fetching SCP content from TaskService ...");
            Response response = feignApi.getTask(jobId);
            int status = response.status();
            if (status != 200) {
                throw new RuntimeException("Failed to fetch SCP (HTTP " + status + ")");
            }
            if (response.body() == null) {
                throw new RuntimeException("Empty SCP response from iDRAC");
            }
            // Read response body properly
            String content;
            try (InputStream is = response.body().asInputStream()) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (content == null || content.isBlank()) {
                throw new RuntimeException("Empty SCP content");
            }
            content = content.trim();

            if (exportFormat == ExportFormat.XML) {
                if (!content.contains("<SystemConfiguration")) {
                    throw new RuntimeException("Response doesn't look like SCP XML.");
                }
            }

            else {
                JsonNode parsed = mapper.readTree(content);

                if (parsed.has("SystemConfiguration") || parsed.has("Components")) {
                    content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
                    System.out.println("Content is:\n" + content);
                }
                else {
                    throw new RuntimeException("Response doesn't look like SCP JSON.");
                }
            }


            System.out.println("[+] SCP content retrieved (" + content.length() + " bytes)");

            return content;

        }
        catch (Exception e) {
            throw new RuntimeException("Failed to fetch SCP: " + e.getMessage(), e);
        }
    }

    @Override
    public void cleanupStuckJobs() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String getSystemModel() {
        try {
            Response response = feignApi.getSystem();
            if (response.status() != 200) {
                throw new RuntimeException("Failed to fetch system info");
            }
            String body;
            try (InputStream is = response.body().asInputStream())
            {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonNode json = mapper.readTree(body);
            return json.path("Model").asText();

        }
        catch (Exception e) {
            throw new RuntimeException("Failed to get system model", e);
        }
    }

    @Override
    public JobResponse getJob(String jobId) {
        return feignApi.getJob(jobId);
    }

    @Override
    public void pollJob(
            String jobId,
            int timeoutSeconds,
            JobProgressListener listener) throws InterruptedException {

        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;

        String lastState = null;
        Integer lastPercent = null;

        while (System.currentTimeMillis() < deadline) {

            Thread.sleep(10000);

            JobResponse job = getJob(jobId);

            String state = job.getJobState();
            Integer percent = job.getPercentComplete();

            boolean changed =
                    !state.equalsIgnoreCase(lastState) ||
                            !Objects.equals(percent, lastPercent);

            if (changed) {

                if (listener != null) {
                    listener.onUpdate(job);
                }

                lastState = state;
                lastPercent = percent;
            }

            if ("Completed".equalsIgnoreCase(state) ||
                    "CompletedWithErrors".equalsIgnoreCase(state)) {

                return;
            }

            if ("Failed".equalsIgnoreCase(state) ||
                    "Exception".equalsIgnoreCase(state) ||
                    "Cancelled".equalsIgnoreCase(state)) {

                throw new RuntimeException(
                        "Job failed: " + state + " — " + job.getMessage());
            }
        }

        throw new RuntimeException("Timeout waiting for job completion");
    }
}