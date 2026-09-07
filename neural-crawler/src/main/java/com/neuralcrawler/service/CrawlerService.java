
/*
  FILE: CrawlerService.java
  ===========================
  The per-source crawl orchestration layer — manages the execution of a single
  source crawl task (GitHub, HackerNews, or Maven Central) within a snapshot run.

  WHAT IT DOES:
  - Receives a CrawlJob (containing source type and target URL) from SchedulerService.
  - Annotated with @Async so each source crawls on its own background thread, allowing
    all three sources to run in parallel within one snapshot run.
  - Manages the CrawlJob lifecycle: sets status to RUNNING, tracks pagesVisited and
    itemsFound as work progresses, sets COMPLETED or FAILED on finish.
  - Coordinates between CrawlerEngine (URL fetching and queuing), the appropriate
    source parser (GitHubTrendingParser / HackerNewsParser / MavenCentralParser),
    NormalizationService (canonicalizing extracted tech names), and
    CrawlResultRepository (persisting TechTrend records).
  - Exposes runSourceCrawl(CrawlJob) — the main async entry point called by SchedulerService.
  - Exposes cancelCrawl(jobId) — signals the engine's stop flag for graceful shutdown.

  WHY IT EXISTS:
  Separates per-source crawl orchestration from the scheduling logic (SchedulerService)
  and the analysis logic (TrendAnalysisService). Each class has one clear responsibility.
  This also makes it straightforward to test a single source crawl in isolation.

  CONNECTS TO:
  - SchedulerService calls runSourceCrawl() for each source job.
  - CrawlerEngine does the actual HTTP fetching and URL queuing.
  - Source parsers are selected and called based on the CrawlJob's source type.
  - NormalizationService.normalize() is called on each extracted TechTrend.
  - CrawlResultRepository.save() persists each extracted TechTrend record.
  - CrawlJob is updated throughout execution and reflects final status on return.
  - AsyncConfig's executor runs this service's @Async methods.
*/
