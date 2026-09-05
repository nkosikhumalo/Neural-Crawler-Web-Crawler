
/*
  FILE: CrawlJob.java
  =====================
  Domain model representing the state and metadata of a single crawl session.

  WHAT IT DOES:
  - Tracks the lifecycle of a crawl from start to finish:
      - jobId        (String / UUID)   — unique identifier for this crawl session
      - targetUrl    (String)          — the seed URL the crawl started from
      - status       (CrawlStatus enum)— PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
      - startedAt    (LocalDateTime)   — when the crawl began
      - completedAt  (LocalDateTime)   — when the crawl finished (or null if still running)
      - pagesVisited (int)             — running count of pages processed so far
      - itemsFound   (int)             — running count of data items extracted so far
      - errorMessage (String)          — populated if the crawl fails with an exception
  - Acts as the status object that the frontend polls to show progress to the user.

  WHY IT EXISTS:
  Without a job model there's no clean way to track multiple crawl sessions, show
  progress, or differentiate a finished crawl from a running one. This object is
  the single source of truth for "what is this crawl doing right now."

  CONNECTS TO:
  - CrawlerService creates and updates a CrawlJob instance throughout the crawl lifecycle.
  - CrawlController reads the CrawlJob to return status responses to the UI.
  - CrawlResultRepository links stored BookItems back to their parent CrawlJob by jobId.
  - CrawlStatus enum is a companion type defined separately or as a nested enum here.
*/
