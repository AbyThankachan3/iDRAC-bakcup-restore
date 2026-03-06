package com.idrac.restore.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreviewResult {

    private List<String> failures;
    private List<String> warnings;
    private List<String> info;

}
