package com.backup.iDRAC.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "server_registration_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerRegistrationJobs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant startedAt;
    private Instant finishedAt;
    private int total;
    private int successCount;
    private int failureCount;
    private String status; // RUNNING COMPLETED FAILED
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    private List<ServerRegFailureLogs> failures;
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    private List<IdracServer> successfulServers;
}

