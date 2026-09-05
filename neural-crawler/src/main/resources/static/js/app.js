
/*
  FILE: app.js
  ==============
  Client-side JavaScript that drives all interactivity in the Neural Crawler web UI.
  This is the frontend "application" layer served as a static asset.

  WHAT IT DOES:
  - Handles the "Start Crawl" form submission: reads the URL and config fields,
    sends a POST /api/crawl request with JSON body, and displays a loading indicator.
  - Starts a polling interval (setInterval) after a crawl begins: every 2 seconds
    it calls GET /api/crawl/status and updates the progress display (status label,
    pages visited, items found). Polling stops when status === "COMPLETED" or "FAILED".
  - On completion (or on user request), calls GET /api/results and dynamically builds
    the results table in the DOM using the returned JSON array of book items.
  - "Download CSV" button navigates to GET /api/export/csv which triggers a file download.
  - "Download JSON" button navigates to GET /api/export/json.
  - "Cancel" button sends DELETE /api/crawl to stop a running crawl.
  - Handles error states: shows user-friendly messages for network errors, validation
    failures (400 responses), and crawl failures.

  WHY IT EXISTS:
  This is what makes the UI feel responsive and alive. Without it, the page would need
  a full reload to show any updates. It bridges the Thymeleaf-served HTML with the
  Spring Boot REST API in a clean, decoupled way.

  CONNECTS TO:
  - index.html includes this script and relies on its DOM manipulation to update the UI.
  - CrawlController's REST endpoints are the backend this script talks to.
  - CrawlResultDTO JSON shape must match what this script expects to parse.
*/
