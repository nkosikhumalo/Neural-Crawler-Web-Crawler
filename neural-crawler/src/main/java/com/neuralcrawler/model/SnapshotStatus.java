package com.neuralcrawler.model;

/*
  FILE: SnapshotStatus.java
  ===========================
  Enum representing the lifecycle state of a TechSnapshot run.

  Values:
  - RUNNING   — snapshot is currently being crawled, not yet complete
  - COMPLETED — all sources finished successfully, full data available
  - PARTIAL   — some sources succeeded, some failed, partial data available
  - FAILED    — all sources failed or a critical error stopped the run entirely

  CONNECTS TO:
  - TechSnapshot.status field uses this enum.
  - SchedulerService sets RUNNING on creation, then COMPLETED/PARTIAL/FAILED on finish.
  - TrendAnalysisService only processes snapshots with COMPLETED or PARTIAL status.
  - CrawlController surfaces this status to the Angular frontend via the snapshot API.
*/
public enum SnapshotStatus {
    RUNNING,
    COMPLETED,
    PARTIAL,
    FAILED
}
