package com.backup.iDRAC.Controller;

import com.backup.iDRAC.Dto.RestoreDiagnosticsResponse;
import com.backup.iDRAC.Dto.RestoreRequest;
import com.backup.iDRAC.Dto.RestoreStartResponse;
import com.backup.iDRAC.Dto.RestoreStatusResponse;
import com.backup.iDRAC.Entity.RestoreLog;
import com.backup.iDRAC.Exception.RestoreIdNotFound;
import com.backup.iDRAC.Repostiory.RestoreLogRepository;
import com.backup.iDRAC.Service.RestoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/restore")
@RequiredArgsConstructor
public class RestoreController {

    private final RestoreService restoreService;
    private final RestoreLogRepository restoreLogRepository;

    @PostMapping("/{backupId}")
    public RestoreStartResponse startRestore(
            @PathVariable Long backupId,
            @RequestBody RestoreRequest request) {

        Long restoreId = restoreService.startRestore(backupId, request.getHost(), request.getUsername(), request.getPassword());
        return RestoreStartResponse.builder()
                .restoreId(restoreId)
                .status("STARTED")
                .build();
    }

    @GetMapping("/{restoreId}")
    public RestoreStatusResponse getStatus(@PathVariable Long restoreId) {

        RestoreLog log = restoreLogRepository.findById(restoreId).orElseThrow(() -> new RestoreIdNotFound(restoreId));
        return RestoreStatusResponse.builder()
                .restoreId(log.getId())
                .status(log.getStatus())
                .percent(log.getPercent())
                .redfishJobId(log.getRedfishJobId())
                .initialHost(log.getInitialHost())
                .finalHost(log.getFinalHost())
                .startedAt(log.getStartedAt())
                .completedAt(log.getCompletedAt())
                .rebootRequired(log.getRebootRequired())
                .build();
    }

    @GetMapping("/{restoreId}/diagnostics")
    public RestoreDiagnosticsResponse getDiagnostics(@PathVariable Long restoreId) {
        return restoreService.getDiagnostics(restoreId);
    }
}
