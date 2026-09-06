package com.neuralcrawler.model;

/*
  FILE: CrawlStatus.java
  ========================
  Enum representing the lifecycle state of a single CrawlJob task.

  Values:
  - PENDING    — job created but not yet started (waiting for a thread)
  - RUNNING    — actively fetching and parsing pages
  - COMPLETED  — finished successfully, all items saved
  - FAILED     — terminated due to an unrecoverable error
  - CANCELLED  — stopped early by a manual cancel request

  CONNECTS TO:
  - CrawlJob.status uses this enum.
  - CrawlerService transitions status through these states during execution.
  - CrawlController returns this status in GET /api/crawl/status/{jobId} responses.
  - SchedulerService checks all job statuses to decide if a snapshot is
    COMPLETED, PARTIAL, or FAILED.
*/
public enum CrawlStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
