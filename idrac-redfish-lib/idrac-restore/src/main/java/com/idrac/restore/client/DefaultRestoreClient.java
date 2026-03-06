package com.idrac.restore.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idrac.restore.model.*;
import com.idrac.restore.transport.FeignRestoreApi;
import feign.Response;
import lombok.RequiredArgsConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RequiredArgsConstructor
public class DefaultRestoreClient implements RestoreClient {

    private final FeignRestoreApi api;
    private final RedfishConnection connection;

    @Override
    public JobResponse preview(String xmlContent) {

        RestoreRequest req = RestoreRequest.builder()
                .importBuffer(xmlContent)
                .shareParameters(Map.of("Target","ALL"))
                .build();

        Response response = api.preview(req);

        String location = response.headers()
                .getOrDefault("Location", List.of())
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No Location header returned"));

        String jobId = location.substring(location.lastIndexOf("/") + 1);

        JobResponse job = new JobResponse();
        job.setId(jobId);

        return job;
    }

    @Override
    public JobResponse importConfig(String xmlContent) {

        RestoreRequest req = RestoreRequest.builder()
                .importBuffer(xmlContent)
                .shutdownType("Graceful")
                .hostPowerState("On")
                .shareParameters(Map.of("Target","ALL"))
                .build();

        Response response = api.importConfiguration(req);

        String location = response.headers()
                .getOrDefault("Location", List.of())
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No Location header returned"));

        String jobId = location.substring(location.lastIndexOf("/") + 1);

        JobResponse job = new JobResponse();
        job.setId(jobId);

        return job;
    }

    @Override
    public JobResponse getJob(String jobId) {
        return api.getJob(jobId);
    }

    //after polling only prints to cli
    @Override
    public void pollJob(String jobId, int timeoutSeconds) throws InterruptedException {

        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        int attempt = 0;
        System.out.println("[*] Polling job " + jobId + " ...");
        while (System.currentTimeMillis() < deadline) {
            attempt++;
            Thread.sleep(10000);

            try {
                JobResponse job = getJob(jobId);
                String state = job.getJobState();
                Integer percent = job.getPercentComplete();
                String message = job.getMessage();
                String percentStr = percent != null ? percent.toString() : "?";
                String messageShort = message != null && message.length() > 60 ? message.substring(0, 60) : message;
                System.out.printf("    [%03d] %-30s %s%%  %s%n", attempt, state, percentStr, messageShort != null ? messageShort : "");

                if ("Completed".equalsIgnoreCase(state) || "CompletedWithErrors".equalsIgnoreCase(state)) {
                    System.out.println("[+] Job finished: " + state);
                    return;
                }

                if ("Failed".equalsIgnoreCase(state) || "Exception".equalsIgnoreCase(state) || "Cancelled".equalsIgnoreCase(state)) {
                    throw new RuntimeException("Job failed: " + state + " — " + message);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Timeout waiting for job completion");
    }

    @Override
    public void pollJob(String jobId, int timeoutSeconds, JobProgressListener listener) throws InterruptedException {

        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;

        String lastState = null;
        Integer lastPercent = null;

        while (System.currentTimeMillis() < deadline) {

            Thread.sleep(10000);

            try {

                JobResponse job = getJob(jobId);

                String state = job.getJobState();
                Integer percent = job.getPercentComplete();
                String message = job.getMessage();

                boolean changed =
                        !state.equalsIgnoreCase(lastState) ||
                                !java.util.Objects.equals(percent, lastPercent);

                if (changed) {

                    if (listener != null) {
                        listener.onUpdate(job);
                    }

                    lastState = state;
                    lastPercent = percent;
                }

                if ("Completed".equalsIgnoreCase(state)
                        || "CompletedWithErrors".equalsIgnoreCase(state)) {

                    return;
                }

                if ("Failed".equalsIgnoreCase(state)
                        || "Exception".equalsIgnoreCase(state)
                        || "Cancelled".equalsIgnoreCase(state)) {

                    throw new RuntimeException(
                            "Job failed: " + state + " — " + message);
                }

            } catch (Exception e) {

                throw new RuntimeException(e);

            }
        }

        throw new RuntimeException("Timeout waiting for job completion");
    }

    @Override
    public RestorePlan analyseXml(String xmlContent, String currentHost) {
        Map<String,String> attrs = parseXmlAttributes(xmlContent);

        NetworkConfig network = extractNetwork(attrs);

        String targetHost = extractTargetHost(attrs, currentHost);

        boolean rebootRequired = requiresShutdown(attrs);

        boolean ipChange = !currentHost.equals(targetHost);

        return RestorePlan.builder()
                .networkConfig(network)
                .currentHost(currentHost)
                .targetHost(targetHost)
                .rebootRequired(rebootRequired)
                .ipChange(ipChange)
                .build();
    }

    @Override
    public RestoreTaskResult fetchTaskResult(String jobId) {

        TaskResponse task = api.getTask(jobId);

        List<RestoreFailure> failures = new ArrayList<>();
        List<TaskMessage> warnings = new ArrayList<>();
        List<JsonNode> rawMessages = new ArrayList<>();

        if (task.getMessages() != null) {
            ObjectMapper mapper = new ObjectMapper();
            for (TaskMessage m : task.getMessages()) {
                JsonNode msgJson = mapper.valueToTree(m);
                rawMessages.add(msgJson);

                boolean failure = false;
                if ("Critical".equalsIgnoreCase(m.getSeverity())) {
                    failure = true;
                }
                JsonNode dell = null;
                if (m.getOem() != null) {
                    dell = m.getOem().get("Dell");
                }
                if (dell != null &&
                        "Failure".equalsIgnoreCase(dell.path("Status").asText())) {
                    failure = true;
                }

                if (failure) {
                    RestoreFailure f = RestoreFailure.builder()
                            .message(m.getMessage())
                            .messageId(m.getMessageId())
                            .severity(m.getSeverity())
                            .attribute(dell != null ? dell.path("Name").asText(null) : null)
                            .fqdd(dell != null ? dell.path("FQDD").asText(null) : null)
                            .errCode(dell != null ? dell.path("ErrCode").asText(null) : null)
                            .build();
                    failures.add(f);
                } else {
                    warnings.add(m);
                }
            }
        }

        return RestoreTaskResult.builder()
                .jobId(jobId)
                .taskState(task.getTaskState())
                .taskStatus(task.getTaskStatus())
                .percentComplete(task.getPercentComplete())
                .failureCount(failures.size())
                .warningCount(warnings.size())
                .failures(failures)
                .warnings(warnings)
                .rawMessages(rawMessages)
                .build();
    }

    public Map<String, String> parseXmlAttributes(String xmlContent) {

        Map<String, String> attrs = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            NodeList nodes = doc.getElementsByTagName("Attribute");

            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                String name = element.getAttribute("Name");
                String value = element.getTextContent().trim();
                attrs.put(name, value);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML", e);
        }
        return attrs;
    }

    public static NetworkConfig extractNetwork(Map<String,String> attrs) {

        boolean dhcp = "Enabled".equalsIgnoreCase(attrs.get("IPv4.1#DHCPEnable"));

        return NetworkConfig.builder()
                .dhcpEnabled(dhcp)
                .staticAddress(attrs.getOrDefault("IPv4Static.1#Address", "N/A"))
                .staticNetmask(attrs.getOrDefault("IPv4Static.1#Netmask", "N/A"))
                .staticGateway(attrs.getOrDefault("IPv4Static.1#Gateway", "N/A"))
                .staticDns1(attrs.getOrDefault("IPv4Static.1#DNS1", "N/A"))
                .staticDns2(attrs.getOrDefault("IPv4Static.1#DNS2", "N/A"))
                .build();
    }

    public static String extractTargetHost(Map<String,String> attrs, String fallback) {

        boolean dhcp = "Enabled".equalsIgnoreCase(attrs.get("IPv4.1#DHCPEnable"));
        if(!dhcp) {
            String ip = attrs.get("IPv4Static.1#Address");
            if(ip != null && !ip.isBlank())
                return ip;
        }
        return fallback;
    }

    public static boolean requiresShutdown(Map<String,String> attrs) {
        final List<String> REBOOT_PREFIXES = List.of(
                "BIOS.",
                "Bios.",
                "Boot.",
                "Proc.",
                "Mem.",
                "RAID.",
                "Storage.",
                "NIC.",
                "SysProfile.",
                "Pci.",
                "PCI.",
                "System."
        );
        return attrs.keySet().stream().anyMatch(key ->
                REBOOT_PREFIXES.stream().anyMatch(key::startsWith)
        );
    }


}
