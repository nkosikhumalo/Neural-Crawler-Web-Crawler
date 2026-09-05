
/*
  FILE: CrawlController.java
  ============================
  The REST controller that acts as the bridge between the Web UI and the backend
  crawling logic.

  WHAT IT DOES:
  - Exposes HTTP endpoints that the frontend JavaScript calls:
      POST /api/crawl        — accepts a URL + config payload, kicks off a new crawl job
      GET  /api/crawl/status — returns the current status and progress of an active crawl
      GET  /api/results      — returns the extracted data items as JSON for the UI to render
      GET  /api/export/csv   — triggers a CSV file download of current results
      GET  /api/export/json  — triggers a JSON file download of current results
  - Delegates all business logic to CrawlerService — the controller itself does nothing
    except validate input, call the service, and format the HTTP response.
  - Returns appropriate HTTP status codes (202 Accepted for async crawl start, 200 for
    data retrieval, 400 for bad input).

  WHY IT EXISTS:
  Follows the MVC/layered architecture pattern — the controller's only job is HTTP
  concerns. Keeping it thin makes it easy to test and swap transport layers later.

  CONNECTS TO:
  - CrawlerService: all method calls are delegated here.
  - CrawlRequestDTO: the incoming POST body is deserialized into this DTO.
  - CrawlResultDTO: outgoing result data is serialized from this DTO.
  - The frontend (index.html + app.js) makes fetch() calls to these endpoints.
*/
