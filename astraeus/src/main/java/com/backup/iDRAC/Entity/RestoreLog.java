package com.backup.iDRAC.Entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity (name = "restore_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long backupJobId;
    private String redfishJobId;
    private String initialHost;
    private String finalHost;
    private String status;
    private Integer percent;
    private Boolean rebootRequired;
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode failures;
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode warnings;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}