package com.neuralcrawler.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/*
  FILE: RateLimiter.java
  ========================
  Per-domain request pacing to keep the crawler polite.
  Enforces a randomized delay between requests to the same domain.

  CONNECTS TO:
  - CrawlerEngine calls acquire(domain) before every HttpFetcherService call.
  - RobotsService calls setDomainDelay(domain, ms) when Crawl-delay is in robots.txt.
  - application.properties supplies min/max delay via crawler.rate.* properties.
*/
@Component
public class RateLimiter {

    @Value("${crawler.rate.min-delay-ms:2000}")
    private long minDelayMs;

    @Value("${crawler.rate.max-delay-ms:5000}")
    private long maxDelayMs;

    // domain → timestamp (ms) of last completed request
    private final ConcurrentHashMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();

    // per-domain delay overrides set by RobotsService from Crawl-delay directives
    private final ConcurrentHashMap<String, Long> domainDelayOverrides = new ConcurrentHashMap<>();

    /**
     * Blocks the calling thread until the required delay has elapsed since the
     * last request to this domain, then records the current time as the new
     * last-request timestamp.
     *
     * Uses randomized jitter between minDelay and maxDelay to avoid patterns
     * that rate-limit detectors can fingerprint.
     */
    public void acquire(String domain) {
        long requiredDelay = domainDelayOverrides.getOrDefault(domain, minDelayMs);
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, maxDelayMs - requiredDelay));
        long totalDelay = requiredDelay + jitter;

        Long last = lastRequestTime.get(domain);
        if (last != null) {
            long elapsed = System.currentTimeMillis() - last;
            long remaining = totalDelay - elapsed;
            if (remaining > 0) {
                try {
                    Thread.sleep(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        lastRequestTime.put(domain, System.currentTimeMillis());
    }

    /**
     * Called by RobotsService when a Crawl-delay directive is found in robots.txt.
     * Overrides the global minDelay for this specific domain.
     * crawlDelaySeconds is converted to milliseconds.
     */
    public void setDomainDelay(String domain, long crawlDelaySeconds) {
        domainDelayOverrides.put(domain, crawlDelaySeconds * 1000);
    }

    /**
     * Removes any override for a domain, reverting it to the global default delay.
     */
    public void clearDomainOverride(String domain) {
        domainDelayOverrides.remove(domain);
    }
}
