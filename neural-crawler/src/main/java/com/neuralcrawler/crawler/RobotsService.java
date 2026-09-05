
/*
  FILE: RobotsService.java
  ==========================
  Utility service that fetches and parses robots.txt files to determine which
  URLs the crawler is permitted to access on a given domain.

  WHAT IT DOES:
  - For a given base domain, fetches <domain>/robots.txt using HttpFetcherService.
  - Parses the Disallow and Allow directives relevant to the crawler's User-Agent.
  - Caches parsed robots rules per domain so we don't re-fetch robots.txt on every
    single request to the same site.
  - Exposes an isAllowed(url) method that CrawlerEngine calls before enqueuing or
    fetching any URL.
  - Also extracts the Crawl-delay directive and surfaces it to the engine for
    politeness delay enforcement.

  WHY IT EXISTS:
  Respecting robots.txt is both an ethical obligation and a legal safeguard for a
  web crawler. This service ensures the crawler doesn't visit pages that site owners
  have marked off-limits, reducing the risk of being blocked or violating terms of service.

  CONNECTS TO:
  - CrawlerEngine consults isAllowed() before processing each URL.
  - HttpFetcherService is used to download the robots.txt file itself.
  - CrawlerConfig injects the configured User-Agent string used to match robot rules.
*/
