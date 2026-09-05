
/*
  FILE: RateLimiter.java
  ========================
  Utility class that enforces a configurable minimum delay between outgoing HTTP
  requests to the same domain (politeness throttling).

  WHAT IT DOES:
  - Tracks the timestamp of the last request made to each domain in a ConcurrentHashMap.
  - Exposes an acquire(domain) method that blocks the calling thread (via Thread.sleep
    or a Guava RateLimiter) until enough time has passed since the last request to
    that domain.
  - Supports per-domain configurable delays — if robots.txt specifies a Crawl-delay
    for a domain, RobotsService passes it here to override the global default.
  - Thread-safe so multiple concurrent crawler threads share the same limiter without
    sending bursts to the same server.

  WHY IT EXISTS:
  Hammering a target server with rapid-fire requests is both rude and likely to get
  the crawler IP-banned. Rate limiting is what separates a polite crawler from a
  denial-of-service attack. This class makes politeness a first-class concern.

  CONNECTS TO:
  - CrawlerEngine calls acquire() before each HTTP fetch.
  - RobotsService feeds Crawl-delay values into this class.
  - CrawlerConfig provides the default global delay setting.
*/
