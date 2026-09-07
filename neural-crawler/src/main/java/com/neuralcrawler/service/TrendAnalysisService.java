
/*
  FILE: TrendAnalysisService.java
  =================================
  Service that computes growth velocity and momentum metrics by comparing
  TechTrend records across consecutive TechSnapshot runs.

  WHAT IT DOES:
  - Called by SchedulerService after each full snapshot run completes.
  - Fetches the two most recent completed TechSnapshot records from the repository.
  - For each canonical tech name present in the current snapshot, finds the matching
    record in the previous snapshot (matched by canonicalName + source).
  - Computes a TrendDelta for each matched pair:
      - starDelta       = currentStars - previousStars
      - growthPercent   = (starDelta / previousStars) * 100
      - mentionDelta    = currentMentions - previousMentions
      - momentum        = RISING if growthPercent > threshold, DECLINING if negative,
                          STABLE otherwise (threshold configurable in application.properties)
  - Handles new entries (no previous record) — marks momentum as NEW.
  - Handles disappeared entries (in previous but not current) — marks momentum as DROPPED.
  - Persists the computed TrendDelta records to the repository.
  - Exposes getTopRising(int limit) and getTopDeclining(int limit) for the controller
    to surface leaderboard data to the Angular frontend.

  WHY IT EXISTS:
  This is the "intelligence" layer of the radar — raw scraped numbers become
  meaningful signals here. Without this service the system is just a periodic scraper;
  with it, the system is a trend detection engine.

  CONNECTS TO:
  - SchedulerService calls computeDeltas() after each snapshot run.
  - CrawlResultRepository provides TechTrend records grouped by snapshotId.
  - TrendDelta model is created and populated here.
  - CrawlController calls getTopRising() / getTopDeclining() for the dashboard API.
  - application.properties supplies the momentum threshold value.
*/
