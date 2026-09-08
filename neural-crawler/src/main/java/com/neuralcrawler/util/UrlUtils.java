package com.neuralcrawler.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*
  FILE: UrlUtils.java
  =====================
  Stateless utility class for URL manipulation and validation.
  All methods are static — no Spring bean needed.

  CONNECTS TO:
  - CrawlerEngine calls isValidCrawlUrl(), isSameDomain(), normalizeUrl().
  - GitHubTrendingParser and HackerNewsParser call normalizeUrl() for relative hrefs.
  - RobotsService calls extractDomain() to key its robots.txt cache.
  - SchedulerService calls buildGitHubTrendingUrl() when seeding the URL list.
*/
public final class UrlUtils {

    private UrlUtils() {}

    /**
     * Resolves a potentially relative URL against a base URL and normalises it:
     * strips fragments, lowercases scheme and host, removes trailing slash.
     * Returns null if resolution fails.
     */
    public static String normalizeUrl(String url, String baseUrl) {
        if (url == null || url.isBlank()) return null;
        try {
            URI base = new URI(baseUrl);
            URI resolved = base.resolve(url.trim());

            // strip fragment
            URI clean = new URI(
                    resolved.getScheme().toLowerCase(),
                    resolved.getUserInfo(),
                    resolved.getHost().toLowerCase(),
                    resolved.getPort(),
                    resolved.getPath(),
                    resolved.getQuery(),
                    null  // no fragment
            );

            String result = clean.toString();
            // remove trailing slash unless it's just the root
            if (result.endsWith("/") && result.lastIndexOf('/') > result.indexOf("://") + 2) {
                result = result.substring(0, result.length() - 1);
            }
            return result;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Returns true if the given URL belongs to the same hostname as targetDomain.
     * Prevents the crawler from following external links off-site.
     */
    public static boolean isSameDomain(String url, String targetDomain) {
        String domain = extractDomain(url);
        return domain != null && domain.equalsIgnoreCase(targetDomain);
    }

    /**
     * Returns true only for well-formed HTTP or HTTPS URLs.
     * Rejects mailto:, javascript:, data:, ftp:, and malformed strings.
     */
    public static boolean isValidCrawlUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Extracts just the hostname from a full URL string.
     * e.g., "https://github.com/trending" → "github.com"
     * Returns null if the URL is malformed.
     */
    public static String extractDomain(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return new URI(url.trim()).getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Builds a GitHub Trending URL for a given language and time period.
     * period must be one of: "daily", "weekly", "monthly"
     * language can be null or empty for the all-languages feed.
     *
     * e.g., buildGitHubTrendingUrl("rust", "daily")
     *       → "https://github.com/trending/rust?since=daily"
     */
    public static String buildGitHubTrendingUrl(String language, String period) {
        StringBuilder sb = new StringBuilder("https://github.com/trending");
        if (language != null && !language.isBlank()) {
            sb.append("/").append(URLEncoder.encode(language.trim(), StandardCharsets.UTF_8));
        }
        sb.append("?since=").append(period != null ? period : "daily");
        return sb.toString();
    }
}
