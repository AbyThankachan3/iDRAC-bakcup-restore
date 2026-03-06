package com.backup.iDRAC.Dto;

import com.idrac.restore.model.RestoreFailure;
import com.idrac.restore.model.TaskMessage;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RestoreDiagnosticsResponse {

    private Long restoreId;
    private Integer failureCount;
    private Integer warningCount;
    private List<RestoreFailure> failures;
    private List<TaskMessage> warnings;
}
