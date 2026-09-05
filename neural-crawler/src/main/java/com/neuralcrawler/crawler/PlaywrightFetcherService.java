
/*
  FILE: PlaywrightFetcherService.java
  =====================================
  Advanced fetcher that uses Playwright for Java to render JavaScript-heavy pages
  before extracting HTML content.

  WHAT IT DOES:
  - Launches a headless Chromium browser instance via the Playwright Java SDK.
  - Navigates to the target URL, waits for the page's JavaScript to execute and the
    DOM to fully render (using waitForLoadState or waitForSelector).
  - Captures the final rendered HTML (page.content()) and returns it as a String —
    the same return type as HttpFetcherService, making it a drop-in swap.
  - Manages browser lifecycle: reuses a single browser context across requests for
    performance, and shuts down cleanly when the crawl ends.
  - Handles common dynamic content patterns: infinite scroll triggers, lazy-loaded
    images, client-side pagination.

  WHY IT EXISTS:
  Standard Jsoup + HttpClient only sees the raw server response HTML. If a site uses
  React, Vue, or Angular to render content client-side, the data simply isn't in the
  raw HTML. This class solves that problem by running a real browser engine.

  CONNECTS TO:
  - CrawlerEngine can be configured (via application.properties) to use this class
    instead of HttpFetcherService for specific domains or as a fallback.
  - HtmlParserService receives the rendered HTML string exactly the same way it
    receives static HTML — no changes needed in the parser.
  - pom.xml must include the Playwright Java dependency.
*/
