package com.idrac.backup.restore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idrac.restore.client.DefaultRestoreClient;
import com.idrac.restore.client.RestoreClient;
import com.idrac.restore.client.RestoreRedfishClientBuilder;
import com.idrac.restore.model.*;
import com.idrac.restore.model.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RestoreTest {

    private static final String HOST = "hostip";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String XML_FILE = "/Users/aby/Downloads/iDRAC/astraeus/astraeus-backups/10.20.21.73/idrac_20260304_162417.xml";

    private RestoreClient createClient() {

        RedfishConnection connection =
                RedfishConnection.builder()
                        .host(HOST)
                        .username(USERNAME)
                        .password(PASSWORD)
                        .build();

        return RestoreRedfishClientBuilder.build(connection);
    }

    @Test
    public void testPreview() throws Exception {

        RestoreClient client = createClient();

        String xml = Files.readString(Path.of(XML_FILE));
        JobResponse preview = client.preview(xml);
        System.out.println("Preview Job ID: " + preview.getId());
        client.pollJob(preview.getId(), 300);
    }

    @Test
    public void testRestoreFlow() throws Exception {

        RestoreClient client = createClient();
        DefaultRestoreClient restore = (DefaultRestoreClient) client;

        String xml = Files.readString(Path.of(XML_FILE));
        Map<String, String> attrs = restore.parseXmlAttributes(xml);
        System.out.println("Parsed attributes: " + attrs.size());
        NetworkConfig network = DefaultRestoreClient.extractNetwork(attrs);
        System.out.println("DHCP Enabled : " + network.isDhcpEnabled());
        System.out.println("Static IP    : " + network.getStaticAddress());
        System.out.println("Gateway      : " + network.getStaticGateway());
        String targetHost = DefaultRestoreClient.extractTargetHost(attrs, HOST);
        System.out.println("Target iDRAC after import: " + targetHost);
        boolean rebootRequired = DefaultRestoreClient.requiresShutdown(attrs);
        System.out.println("Reboot required: " + rebootRequired);
        JobResponse preview = client.preview(xml);
        System.out.println("Preview Job ID: " + preview.getId());
        client.pollJob(preview.getId(), 300);
        JobResponse importJob = client.importConfig(xml);
        System.out.println("Import Job ID: " + importJob.getId());
        client.pollJob(importJob.getId(), 600);
        System.out.println("Restore completed.");
    }

    @Test
    public void testFetchTaskResult() throws Exception {

        String jobId = "JID_727942878518";
        RestoreClient client = createClient();
        RestoreTaskResult result = client.fetchTaskResult(jobId);

        System.out.println("=====================================");
        System.out.println("Job ID       : " + result.getJobId());
        System.out.println("Task State   : " + result.getTaskState());
        System.out.println("Task Status  : " + result.getTaskStatus());
        System.out.println("Percent      : " + result.getPercentComplete());
        System.out.println("=====================================");
        System.out.println("Failures (" + result.getFailureCount() + "):");

        List<RestoreFailure> failures = result.getFailures();
        for (RestoreFailure f : failures) {

            System.out.println("-------------------------------------");
            System.out.println("Message  : " + f.getMessage());
            System.out.println("Severity : " + f.getSeverity());
            System.out.println("Attr     : " + f.getAttribute());
            System.out.println("FQDD     : " + f.getFqdd());
            System.out.println("ErrCode  : " + f.getErrCode());
        }

        System.out.println("=====================================");
        System.out.println("Warnings (" + result.getWarningCount() + "):");
        result.getWarnings().forEach(w -> {
            System.out.println("-------------------------------------");
            System.out.println("Message  : " + w.getMessage());
            System.out.println("Severity : " + w.getSeverity());
        });

        System.out.println("=====================================");
        System.out.println("Raw Messages Count : " + result.getRawMessages().size());
        System.out.println("=====================================");

        System.out.println("RAW MESSAGES:");

        ObjectMapper mapper = new ObjectMapper();

        result.getRawMessages().forEach(msg -> {
            try {
                System.out.println("-------------------------------------");
                System.out.println(
                        mapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(msg)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.out.println("=====================================");
    }

}