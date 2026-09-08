package com.neuralcrawler.service;

import com.neuralcrawler.dao.CrawlResultRepository;
import com.neuralcrawler.model.*;
import com.neuralcrawler.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/*
  FILE: SchedulerService.java
  =============================
  Top-level orchestrator. Fires a full multi-source radar run on a cron schedule
  and exposes manualTrigger() for on-demand runs from the Angular dashboard.

  CONNECTS TO:
  - CrawlerService.runSourceCrawl() is called here for each source job.
  - TrendAnalysisService.computeDeltas() is called after all sources complete.
  - CrawlResultRepository stores the TechSnapshot lifecycle updates.
  - AsyncConfig activates @Scheduled via @EnableScheduling.
  - application.properties supplies the cron expression and source seed URLs.
*/
@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    @Value("${radar.schedule.enabled:true}")
    private boolean scheduleEnabled;

    @Value("${radar.sources.github.urls:https://github.com/trending}")
    private String githubUrlsCsv;

    @Value("${radar.sources.hackernews.api-url:https://hacker-news.firebaseio.com/v0/topstories.json}")
    private String hnApiUrl;

    @Value("${radar.sources.hackernews.ask-url:https://hacker-news.firebaseio.com/v0/askstories.json}")
    private String hnAskUrl;

    @Value("${radar.sources.maven.api-url:https://search.maven.org/solrsearch/select?q=spring-boot&rows=20&wt=json}")
    private String mavenUrl;

    @Value("${radar.sources.stackexchange.api-url:https://api.stackexchange.com/2.3/tags?order=desc&sort=popular&site=stackoverflow&pagesize=50&filter=default}")
    private String stackExchangeUrl;

    @Value("${radar.scheduler.timeout-minutes:10}")
    private int timeoutMinutes;

    private final CrawlerService crawlerService;
    private final TrendAnalysisService analysisService;
    private final CrawlResultRepository repository;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public SchedulerService(CrawlerService crawlerService,
                            TrendAnalysisService analysisService,
                            CrawlResultRepository repository) {
        this.crawlerService = crawlerService;
        this.analysisService = analysisService;
        this.repository = repository;
    }

    /** Triggered automatically every 6 hours by the cron expression in application.properties. */
    @Scheduled(cron = "${radar.schedule.cron:0 0 0/6 * * *}")
    public void scheduledRun() {
        if (!scheduleEnabled) {
            log.debug("Scheduled runs are disabled (radar.schedule.enabled=false).");
            return;
        }
        triggerRun("SCHEDULER", null);
    }

    /** Returns true if no crawl is currently running — safe to call from the controller. */
    public boolean isAvailable() {
        return !running.get();
    }

    /** Called by CrawlController — starts the crawl on a background thread immediately. */
    public void manualTrigger(List<String> requestedSources) {
        // Run on a new daemon thread so the HTTP response returns instantly
        Thread crawlThread = new Thread(() -> triggerRun("MANUAL", requestedSources), "manual-trigger");
        crawlThread.setDaemon(true);
        crawlThread.start();
    }

    private TechSnapshot triggerRun(String triggeredBy, List<String> requestedSources) {
        if (!running.compareAndSet(false, true)) {
            log.warn("Radar run skipped — a crawl is already in progress.");
            return null;
        }

        List<String> allSources = List.of(
                TrendSource.GITHUB.name(),
                TrendSource.HACKERNEWS_API.name(),
                TrendSource.MAVEN_CENTRAL.name(),
                TrendSource.STACKOVERFLOW_JOBS.name()
        );
        List<String> sources = requestedSources == null || requestedSources.isEmpty()
            ? allSources
            : requestedSources.stream()
                .map(String::toUpperCase)
                .filter(allSources::contains)
                .distinct()
                .toList();
        if (sources.isEmpty()) sources = allSources;

        TechSnapshot snapshot = TechSnapshot.startNew(triggeredBy, sources);
        repository.saveSnapshot(snapshot);
        log.info("Radar run started. Snapshot: {}, triggeredBy: {}", snapshot.getSnapshotId(), triggeredBy);

        try {
            List<CompletableFuture<CrawlJob>> futures = new ArrayList<>();

            // GitHub Trending — one job per configured URL
            if (sources.contains(TrendSource.GITHUB.name())) {
                for (String url : githubUrlsCsv.split(",")) {
                    CrawlJob job = CrawlJob.create(snapshot.getSnapshotId(), TrendSource.GITHUB);
                    futures.add(crawlerService.runSourceCrawl(job, List.of(url.trim())));
                }
            }

            // HackerNews official Firebase API (top stories + Ask HN)
            if (sources.contains(TrendSource.HACKERNEWS_API.name())) {
                CrawlJob hnJob = CrawlJob.create(snapshot.getSnapshotId(), TrendSource.HACKERNEWS_API);
                futures.add(crawlerService.runSourceCrawl(hnJob, List.of(hnApiUrl, hnAskUrl)));
            }

            // Maven Central search API
            if (sources.contains(TrendSource.MAVEN_CENTRAL.name())) {
                CrawlJob mavenJob = CrawlJob.create(snapshot.getSnapshotId(), TrendSource.MAVEN_CENTRAL);
                futures.add(crawlerService.runSourceCrawl(mavenJob, List.of(mavenUrl)));
            }

            // Stack Exchange Tags API (most popular tags on Stack Overflow — free, no auth)
            if (sources.contains(TrendSource.STACKOVERFLOW_JOBS.name())) {
                CrawlJob soJob = CrawlJob.create(snapshot.getSnapshotId(), TrendSource.STACKOVERFLOW_JOBS);
                futures.add(crawlerService.runSourceCrawl(soJob, List.of(stackExchangeUrl)));
            }

            // Wait for all source crawls to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeoutMinutes, TimeUnit.MINUTES);

            long failedJobs = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(job -> job.getStatus() == CrawlStatus.FAILED)
                    .count();

            int totalItems = repository.findTrendsBySnapshot(snapshot.getSnapshotId()).size();

            if (failedJobs == 0) {
                snapshot.markCompleted(totalItems);
            } else if (failedJobs < futures.size()) {
                snapshot.markPartial(totalItems, failedJobs + " source(s) failed.");
            } else {
                snapshot.markFailed("All sources failed.");
            }

            repository.saveSnapshot(snapshot);
            log.info("Snapshot {} finished with status {}. Items: {}",
                    snapshot.getSnapshotId(), snapshot.getStatus(), totalItems);

            // Compute trend deltas now that the snapshot is complete
            analysisService.computeDeltas(snapshot.getSnapshotId());

        } catch (Exception e) {
            snapshot.markFailed(e.getMessage());
            repository.saveSnapshot(snapshot);
            log.error("Radar run failed for snapshot {}: {}", snapshot.getSnapshotId(), e.getMessage(), e);
        } finally {
            running.set(false);
        }

        return snapshot;
    }
}
