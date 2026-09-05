
/*
  FILE: CrawlerEngine.java
  ==========================
  The low-level crawling engine — responsible for fetching web pages and managing
  the URL frontier (the queue of pages still to visit).

  WHAT IT DOES:
  - Maintains a ConcurrentLinkedQueue of URLs to visit (the frontier) and a
    ConcurrentHashMap / Set of already-visited URLs to prevent revisiting the same page.
  - Pulls URLs from the queue, uses HttpFetcherService to download the raw HTML,
    and passes the HTML to HtmlParserService for data extraction and link discovery.
  - Implements crawl depth tracking: each URL in the queue carries a depth counter;
    when it exceeds the configured max depth, that URL is skipped.
  - Enforces politeness delay between requests (Thread.sleep or a rate limiter).
  - Discovers and enqueues pagination links and category links found during parsing
    so the crawl fans out across the site automatically.
  - Respects a stop signal (volatile boolean or AtomicBoolean) so the crawl can be
    cancelled cleanly from the service layer.

  WHY IT EXISTS:
  This is the Scrapy Spider equivalent — the part that actually "crawls." Separating
  fetch-and-queue logic from parsing and storage keeps each piece focused and swappable.

  CONNECTS TO:
  - CrawlerService creates/starts/stops this engine.
  - HttpFetcherService is called here to retrieve raw HTML for each URL.
  - HtmlParserService processes the fetched HTML and returns extracted items + new links.
  - CrawlerConfig injects the thread pool and politeness delay settings.
  - RobotsService is consulted here before fetching any URL.
*/
