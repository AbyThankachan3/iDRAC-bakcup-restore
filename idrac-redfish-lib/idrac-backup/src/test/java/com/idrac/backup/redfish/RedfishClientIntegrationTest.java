package com.idrac.backup.redfish;

import com.idrac.backup.api.RedfishClient;
import com.idrac.backup.client.RedfishClientBuilder;
import com.idrac.backup.config.RedfishConnection;
import com.idrac.backup.model.ExportFormat;
import com.idrac.backup.model.ExportTarget;
import com.idrac.backup.model.ExportUse;
import com.idrac.backup.model.IncludeInExport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedfishClientIntegrationTest {

    private static final String HOST = "hostip";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final long TIMEOUT = 300000;


    private RedfishClient createClient() {

        RedfishConnection connection =
                RedfishConnection.builder()
                        .host(HOST)
                        .username(USERNAME)
                        .password(PASSWORD)
                        .build();

        return RedfishClientBuilder.build(connection);
    }

    @Test
    public void testExportAndDownloadScp() throws Exception {

        RedfishClient client = createClient();

        // Trigger export
        String jobId = client.triggerExport(ExportTarget.ALL, ExportFormat.XML, ExportUse.REPLACE, IncludeInExport.INCLUDE_READ_ONLY,
                IncludeInExport.INCLUDE_PASSWORD_HASH_VALUES);
        assertNotNull(jobId);
        System.out.println("Job ID: " + jobId);

        // Poll job
        client.pollJob(jobId, TIMEOUT);

        // Fetch SCP
        String scp = client.fetchScp(jobId, ExportFormat.XML);
        assertNotNull(scp);
        assertTrue(scp.contains("<SystemConfiguration"), "Invalid SCP content");

        // Save to file
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "target/idrac_" + HOST + "_" + timestamp + ".xml";

        Path path = Path.of(filename);
        Files.createDirectories(path.getParent());
        Files.writeString(path, scp, StandardCharsets.UTF_8);
        System.out.println("[+] SCP saved to: " + path.toAbsolutePath());
        assertTrue(Files.exists(path));

    }

    @Test
    public void getSystemModelTest(){
        RedfishClient client = createClient();
        System.out.println(client.getSystemModel());
    }

}