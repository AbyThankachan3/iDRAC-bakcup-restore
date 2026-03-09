package com.backup.iDRAC.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "idrac_servers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdracServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String host;
    @Column(nullable = false)
    private String model;
    @Column(nullable = false)
    private String vaultPath;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ServerRegistrationJobs job;
}