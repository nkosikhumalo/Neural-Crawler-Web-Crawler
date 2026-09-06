package com.neuralcrawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/*
  FILE: CrawlJob.java
  =====================
  Domain model tracking the execution state of a single source crawl task
  within a scheduled radar run. One TechSnapshot spawns one CrawlJob per source.

  CONNECTS TO:
  - SchedulerService creates instances via CrawlJob.create().
  - CrawlerService updates pagesVisited, itemsFound, and status during execution.
  - CrawlController exposes status via GET /api/crawl/status/{jobId}.
  - CrawlResultRepository links TechTrend records to a job via snapshotId.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlJob {

    private String jobId;
    private String snapshotId;
    private TrendSource source;
    private CrawlStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int pagesVisited;
    private int itemsFound;
    private String errorMessage;

    /**
     * Factory method — creates a new PENDING job for a given source and snapshot.
     */
    public static CrawlJob create(String snapshotId, TrendSource source) {
        return CrawlJob.builder()
                .jobId(UUID.randomUUID().toString())
                .snapshotId(snapshotId)
                .source(source)
                .status(CrawlStatus.PENDING)
                .pagesVisited(0)
                .itemsFound(0)
                .build();
    }

    /**
     * Transitions the job to RUNNING and records the start time.
     */
    public void start() {
        this.status = CrawlStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Transitions the job to COMPLETED and records the finish time.
     */
    public void complete(int totalItems) {
        this.status = CrawlStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.itemsFound = totalItems;
    }

    /**
     * Transitions the job to FAILED with an error message.
     */
    public void fail(String error) {
        this.status = CrawlStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = error;
    }

    /**
     * Transitions the job to CANCELLED.
     */
    public void cancel() {
        this.status = CrawlStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Increments page and item counters during active crawling.
     */
    public void recordProgress(int newItems) {
        this.pagesVisited++;
        this.itemsFound += newItems;
    }
}
