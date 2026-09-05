
/*
  FILE: CrawlResultDTO.java
  ===========================
  Data Transfer Object that shapes the outgoing response payload sent from the
  backend to the frontend when results or status are requested.

  WHAT IT DOES:
  - Carries the data the UI needs to render a results table:
      - jobId        (String)          — which crawl these results belong to
      - status       (String)          — current crawl status label
      - pagesVisited (int)             — progress indicator
      - itemsFound   (int)             — total items extracted
      - items        (List<BookItem>)  — the actual extracted records
      - message      (String)          — human-readable status or error message
  - Jackson serializes this into the JSON response body automatically.

  WHY IT EXISTS:
  Keeps the API response shape stable and independent from internal model changes.
  If BookItem gains new internal fields that shouldn't be exposed to the frontend,
  the DTO layer is where you control what gets included.

  CONNECTS TO:
  - CrawlController builds and returns this DTO from GET /api/results and GET /api/crawl/status.
  - CrawlerService populates CrawlJob and BookItem data that gets mapped into this DTO.
  - The frontend JavaScript (app.js) parses this JSON structure to update the UI.
*/
