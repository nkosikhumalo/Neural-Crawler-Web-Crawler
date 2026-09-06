package com.neuralcrawler.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tech_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechSnapshot {

    @Id
    @Column(nullable = false, updatable = false)
    private String snapshotId;

    private String triggeredBy; // e.g., "SCHEDULER" or "MANUAL"

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "snapshot_sources", joinColumns = @JoinColumn(name = "snapshot_id"))
    @Column(name = "source_name")
    @Builder.Default
    private List<String> sourcesRan = new ArrayList<>();

    private int totalItems;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SnapshotStatus status;

    @Column(length = 2000)
    private String errorNotes;

    /**
     * Factory method to initialize a new RUNNING snapshot instance.
     */
    public static TechSnapshot startNew(String triggeredBy, List<String> sources) {
        return TechSnapshot.builder()
                .snapshotId(UUID.randomUUID().toString())
                .triggeredBy(triggeredBy)
                .startedAt(LocalDateTime.now())
                .sourcesRan(sources != null ? sources : new ArrayList<>())
                .status(SnapshotStatus.RUNNING)
                .totalItems(0)
                .build();
    }

    /**
     * Lifecycle helper method to transition the snapshot to COMPLETED state.
     */
    public void markCompleted(int itemCount) {
        this.status = SnapshotStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.totalItems = itemCount;
    }

    /**
     * Lifecycle helper — some sources succeeded, some failed. Partial data available.
     */
    public void markPartial(int itemCount, String notes) {
        this.status = SnapshotStatus.PARTIAL;
        this.completedAt = LocalDateTime.now();
        this.totalItems = itemCount;
        this.errorNotes = notes;
    }

    /**
     * Lifecycle helper method to transition the snapshot to FAILED state.
     */
    public void markFailed(String errorMessage) {
        this.status = SnapshotStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorNotes = errorMessage;
    }
}


