
/*
  FILE: CrawlerConfig.java
  ==========================
  Spring configuration class that defines application-wide infrastructure beans
  for the crawling engine.

  WHAT IT DOES:
  - Declares and configures the ExecutorService / ThreadPoolExecutor bean that the
    crawler uses to run per-source fetch-and-parse tasks concurrently.
    Minimum 3 threads recommended — one per source (GitHub, HackerNews, Maven).
  - Registers an Apache HttpClient 5 bean with:
      - Shared connection pool (max connections per route and total)
      - Custom User-Agent header identifying the crawler politely
      - Configurable connect timeout and socket read timeout
      - Automatic redirect following up to a configurable max
      - Optional proxy support for environments behind a firewall
  - Sets configurable properties injected from application.properties:
      - crawler.thread-pool.size      — number of concurrent crawler threads
      - crawler.http.connect-timeout  — HTTP connect timeout in milliseconds
      - crawler.http.read-timeout     — HTTP read timeout in milliseconds
      - crawler.user-agent            — User-Agent string for all requests

  WHY IT EXISTS:
  Thread pools and HTTP clients are expensive to create and should be singletons
  shared across the app. Spring bean lifecycle manages this perfectly. Keeping
  infrastructure setup here keeps CrawlerEngine and fetcher classes clean.

  CONNECTS TO:
  - CrawlerEngine injects the ExecutorService and HttpClient beans from here.
  - HttpFetcherService injects the shared HttpClient bean.
  - AsyncConfig works alongside this to enable @Async on the service layer.
  - application.properties feeds all the configurable values.
*/
