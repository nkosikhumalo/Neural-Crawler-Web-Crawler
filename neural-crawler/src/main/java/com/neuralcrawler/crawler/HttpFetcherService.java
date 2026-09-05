
/*
  FILE: HttpFetcherService.java
  ===============================
  Low-level HTTP layer — the only class that directly makes network requests.

  WHAT IT DOES:
  - Wraps Apache HttpClient (or Java's native HttpClient) and exposes a clean
    fetchPage(url) method that returns raw HTML as a String.
  - Sets a custom User-Agent header on every request to identify the crawler politely
    (or mimic a browser where needed).
  - Handles HTTP redirects automatically and follows them up to a configurable limit.
  - Applies request timeouts (connect timeout, socket read timeout) to avoid hanging
    indefinitely on slow servers.
  - Handles error responses: 404 returns empty / null, 429 (rate limited) triggers a
    back-off and retry, 5xx server errors are logged and skipped.
  - Optionally manages cookies / sessions if the target site requires them.

  WHY IT EXISTS:
  Isolating all HTTP concerns in one class makes it trivial to swap the underlying
  client library, add proxy support, or inject mock responses in tests without touching
  any parsing or crawling logic.

  CONNECTS TO:
  - CrawlerEngine calls fetchPage() for every URL in the frontier.
  - PlaywrightFetcherService can be used as a drop-in replacement when JavaScript
    rendering is required.
  - CrawlerConfig provides the shared HttpClient bean injected here.
  - RobotsService checks are done by CrawlerEngine before calling this class.
*/
