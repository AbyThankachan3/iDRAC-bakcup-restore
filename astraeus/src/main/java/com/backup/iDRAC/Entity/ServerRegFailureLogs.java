package com.backup.iDRAC.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "server_register_failures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerRegFailureLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private ServerRegistrationJobs job;
    private String host;
    private String error;
}
