
/*
  FILE: CrawlerService.java
  ===========================
  The core orchestration layer — the brain of the application. Sits between the
  controller and the low-level crawler/parser components.

  WHAT IT DOES:
  - Receives a crawl request (target URL, depth, selector config) from the controller.
  - Annotated with @Async so when the controller calls startCrawl(), it immediately
    returns a CompletableFuture while crawling runs on a background thread.
  - Manages crawl job lifecycle: initializing state, tracking progress percentage,
    handling completion or failure, and making results available to the controller.
  - Coordinates between CrawlerEngine (fetching), HtmlParserService (extracting data),
    and CrawlResultRepository/DAO (storing results).
  - Exposes methods: startCrawl(CrawlRequestDTO), getStatus(), getResults(), cancelCrawl().

  WHY IT EXISTS:
  The service layer enforces the single-responsibility principle. Business rules and
  workflow logic live here, not in controllers (HTTP concerns) or crawlers (I/O concerns).
  This also makes the business logic independently testable with mocked dependencies.

  CONNECTS TO:
  - CrawlController calls into this service.
  - CrawlerEngine is called from here to do the actual HTTP fetching.
  - HtmlParserService is called from here after pages are fetched.
  - CrawlResultRepository stores the extracted BookItem / generic item data.
  - ExportService is called from here when the controller requests an export.
  - AsyncConfig's executor runs this service's @Async methods.
*/
