package com.neuralcrawler.crawler;

import com.neuralcrawler.util.RateLimiter;
import com.neuralcrawler.util.RequestSecurityUtils;
import com.neuralcrawler.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
  FILE: HttpFetcherService.java
  ===============================
  The only class that makes direct network requests. All fetch calls go through
  here, with security hardening applied on every request.

  SECURITY MEASURES:
  - HTTPS enforced on all outbound URLs (HTTP upgraded automatically)
  - User-Agent rotated per request from a realistic browser pool
  - Accept-Language and Accept headers randomized to reduce fingerprinting
  - 429 backoff with Retry-After header respected
  - Rate limiting applied before every domain request
*/
@Service
public class HttpFetcherService {

    private static final Logger log = LoggerFactory.getLogger(HttpFetcherService.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_AFTER_DEFAULT_MS = 60_000;

    private final CloseableHttpClient httpClient;
    private final RateLimiter rateLimiter;
    private final RequestSecurityUtils security;

    public HttpFetcherService(CloseableHttpClient httpClient,
                               RateLimiter rateLimiter,
                               RequestSecurityUtils security) {
        this.httpClient = httpClient;
        this.rateLimiter = rateLimiter;
        this.security = security;
    }

    public String fetch(String url) {
        return fetch(url, MAX_RETRIES);
    }

    private String fetch(String url, int retriesLeft) {
        // Enforce HTTPS — never fetch over plain HTTP
        url = security.enforceHttps(url);
        if (!security.isSecureUrl(url)) {
            log.warn("Rejecting non-HTTPS URL: {}", url);
            return null;
        }

        String domain = UrlUtils.extractDomain(url);
        if (domain != null) rateLimiter.acquire(domain);

        HttpGet request = new HttpGet(url);

        // Rotate headers per request for reduced fingerprinting
        request.setHeader(HttpHeaders.USER_AGENT, security.getUserAgent());
        request.setHeader(HttpHeaders.ACCEPT_LANGUAGE, security.getAcceptLanguage());
        request.setHeader(HttpHeaders.ACCEPT, security.getAcceptHeader());
        request.setHeader("DNT", "1");
        request.setHeader("Upgrade-Insecure-Requests", "1");

        try {
            int[] statusHolder = new int[1];
            long[] retryAfterHolder = new long[]{RETRY_AFTER_DEFAULT_MS};

            String body = httpClient.execute(request, response -> {
                statusHolder[0] = response.getCode();
                if (statusHolder[0] == HttpStatus.SC_OK) {
                    return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }
                if (statusHolder[0] == 429) {
                    retryAfterHolder[0] = parseRetryAfter(
                            response.getFirstHeader("Retry-After"));
                }
                return null;
            });

            int status = statusHolder[0];
            if (status == HttpStatus.SC_OK) return body;
            if (status == HttpStatus.SC_NOT_FOUND) { log.warn("404: {}", url); return null; }
            if (status == 429) {
                if (retriesLeft > 0) {
                    log.warn("429 on {}. Backing off {}ms.", url, retryAfterHolder[0]);
                    Thread.sleep(retryAfterHolder[0]);
                    return fetch(url, retriesLeft - 1);
                }
                log.error("429 on {} — retries exhausted.", url);
                return null;
            }
            if (status >= 500) { log.error("Server error {} on {}", status, url); return null; }
            log.warn("Unexpected status {} for {}", status, url);
            return null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            if (retriesLeft > 0) {
                long retryDelay = 1_000L * (MAX_RETRIES - retriesLeft + 1);
                log.warn("IO error fetching {}. Retrying in {}ms.", url, retryDelay);
                try {
                    Thread.sleep(retryDelay);
                    return fetch(url, retriesLeft - 1);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            log.error("IO error fetching {}: {}", url, e.getMessage());
            return null;
        }
    }

    private long parseRetryAfter(org.apache.hc.core5.http.Header header) {
        if (header == null) return RETRY_AFTER_DEFAULT_MS;
        try { return Long.parseLong(header.getValue().trim()) * 1000; }
        catch (NumberFormatException e) { return RETRY_AFTER_DEFAULT_MS; }
    }
}
