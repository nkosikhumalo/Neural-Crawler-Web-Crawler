package com.neuralcrawler.util;

import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/*
  FILE: RequestSecurityUtils.java
  =================================
  Security utility for outbound HTTP requests and inbound data sanitization.

  WHAT IT DOES:
  - Rotates User-Agent strings from a realistic browser pool so the crawler
    does not present a consistent fingerprint to target servers.
  - Provides randomized Accept-Language and Accept headers to further reduce
    detectability as an automated client.
  - Sanitizes all scraped string data before storage — strips HTML tags,
    script content, and control characters that could cause injection issues
    if the data is ever rendered in a UI or stored in a database.
  - Enforces HTTPS-only policy — rejects any URL using plain HTTP.

  SECURITY RATIONALE:
  - User-Agent rotation: prevents trivial bot detection based on a fixed UA string
  - Header randomization: reduces statistical fingerprinting across requests
  - Input sanitization: prevents stored XSS if scraped content reaches the frontend
  - HTTPS enforcement: ensures all outbound data in transit is encrypted,
    preventing MITM interception of scraped responses

  CONNECTS TO:
  - HttpFetcherService calls getUserAgent() and getAcceptLanguage() per request.
  - All parsers call sanitize() on extracted text before populating TechTrend fields.
*/
@Component
public class RequestSecurityUtils {

    // Realistic browser User-Agents — rotated per request
    private static final List<String> USER_AGENTS = List.of(
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64; rv:125.0) Gecko/20100101 Firefox/125.0",
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:124.0) Gecko/20100101 Firefox/124.0",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36 Edg/123.0.0.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"
    );

    private static final List<String> ACCEPT_LANGUAGES = List.of(
        "en-US,en;q=0.9",
        "en-GB,en;q=0.9",
        "en-US,en;q=0.8,de;q=0.6",
        "en-US,en;q=0.9,fr;q=0.7",
        "en;q=0.9"
    );

    private static final List<String> ACCEPT_HEADERS = List.of(
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
    );

    /** Returns a randomly selected User-Agent string from the rotation pool. */
    public String getUserAgent() {
        return USER_AGENTS.get(ThreadLocalRandom.current().nextInt(USER_AGENTS.size()));
    }

    /** Returns a randomly selected Accept-Language header value. */
    public String getAcceptLanguage() {
        return ACCEPT_LANGUAGES.get(ThreadLocalRandom.current().nextInt(ACCEPT_LANGUAGES.size()));
    }

    /** Returns a randomly selected Accept header value. */
    public String getAcceptHeader() {
        return ACCEPT_HEADERS.get(ThreadLocalRandom.current().nextInt(ACCEPT_HEADERS.size()));
    }

    /**
     * Sanitizes a scraped string before storage.
     * - Strips all HTML tags and attributes
     * - Removes control characters (null bytes, etc.)
     * - Trims to a maximum length to prevent oversized payloads
     * - Returns empty string for null input
     */
    public String sanitize(String raw) {
        if (raw == null) return "";
        // Strip HTML using Jsoup's safe list (no tags allowed)
        String stripped = org.jsoup.Jsoup.clean(raw, Safelist.none());
        // Remove control characters except normal whitespace
        stripped = stripped.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        // Trim to max 2000 chars
        return stripped.length() > 2000 ? stripped.substring(0, 2000) : stripped.trim();
    }

    /**
     * Enforces HTTPS-only policy for outbound requests.
     * Returns true if the URL uses HTTPS, false if HTTP or anything else.
     * Crawling plain HTTP URLs risks MITM interception of responses.
     */
    public boolean isSecureUrl(String url) {
        if (url == null) return false;
        return url.toLowerCase().startsWith("https://");
    }

    /**
     * Upgrades an HTTP URL to HTTPS if possible.
     * Only applies the upgrade — does not verify the server supports HTTPS.
     */
    public String enforceHttps(String url) {
        if (url == null) return null;
        if (url.toLowerCase().startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }
}
