package com.backup.iDRAC.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bulk_register_failures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkRegisterFailureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private BulkRegisterJob job;
    private String host;
    private String error;
}
