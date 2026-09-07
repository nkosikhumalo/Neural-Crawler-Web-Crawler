
/*
  FILE: SchedulerService.java
  =============================
  The automated scheduling layer that triggers radar crawl runs on a fixed cron
  schedule without any user interaction required.

  WHAT IT DOES:
  - Annotated with @EnableScheduling (or relies on it being set in AsyncConfig) and
    uses @Scheduled(cron = "${radar.schedule.cron}") to fire a full radar crawl run
    at configurable intervals (default every 6 hours, cron: 0 0 every6 * * *).
  - At each scheduled trigger:
      1. Creates a new TechSnapshot record and persists it with RUNNING status.
      2. Spawns one CrawlJob per configured source (GitHub, HackerNews, Maven Central).
      3. Calls CrawlerService.runSourceCrawl() for each job asynchronously so all
         three sources crawl in parallel rather than sequentially.
      4. Waits for all CompletableFutures to complete (with a configurable timeout).
      5. Updates the TechSnapshot to COMPLETED (or PARTIAL if some sources failed).
      6. Calls TrendAnalysisService.computeDeltas() to generate fresh TrendDelta records.
  - Also exposes a manualTrigger() method that CrawlController can call when the
    frontend user hits "Run Now" — same logic, triggered = "MANUAL".
  - Prevents overlapping runs: if a crawl is still running when the next schedule fires,
    the new run is skipped and a warning is logged.

  WHY IT EXISTS:
  The shift from on-demand to scheduled execution is the core architectural change
  from the books crawler. Trend data has no value if it's only fresh when someone
  manually clicks a button. This service is what keeps the radar current automatically.

  CONNECTS TO:
  - CrawlerService.runSourceCrawl() is called here for each source.
  - TechSnapshot is created and updated by this service.
  - TrendAnalysisService.computeDeltas() is called here after each run completes.
  - CrawlController calls manualTrigger() for the "Run Now" UI button.
  - application.properties supplies the cron expression via radar.schedule.cron.
  - AsyncConfig's executor handles the parallel async execution of source crawls.
*/
