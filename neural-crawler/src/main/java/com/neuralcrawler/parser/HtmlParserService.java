package com.neuralcrawler.parser;

import com.neuralcrawler.model.TechTrend;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;

/*
  FILE: HtmlParserService.java
  ==============================
  Abstract base class defining the parse contract and shared utility methods
  for all source-specific parsers. CrawlerEngine calls parse() on whichever
  subclass matches the current source.

  CONNECTS TO:
  - GitHubTrendingParser, HackerNewsParser, MavenCentralParser extend this.
  - CrawlerEngine uses this type to call any parser uniformly.
  - ParseResult is the return type carrying extracted items and follow-up URLs.
*/
public abstract class HtmlParserService {

    /**
     * Parse the given raw HTML string from sourceUrl and return extracted
     * TechTrend records plus any follow-up URLs to enqueue.
     *
     * @param html      raw HTML content of the fetched page
     * @param sourceUrl the URL this HTML was fetched from
     * @param snapshotId current snapshot ID to stamp on each TechTrend
     */
    public abstract ParseResult parse(String html, String sourceUrl, String snapshotId);

    // -------------------------------------------------------------------------
    // Shared utility methods available to all subclasses
    // -------------------------------------------------------------------------

    /**
     * Strips leading/trailing whitespace, collapses internal whitespace,
     * and removes non-breaking spaces. Safe to call on null — returns empty string.
     */
    protected String cleanText(String raw) {
        if (raw == null) return "";
        return raw.replace("\u00a0", " ").trim().replaceAll("\\s+", " ");
    }

    /**
     * Safely selects the first matching element and returns its text content.
     * Returns empty string if selector matches nothing — never throws.
     */
    protected String safeText(Document doc, String cssSelector) {
        Element el = doc.selectFirst(cssSelector);
        return el != null ? cleanText(el.text()) : "";
    }

    /**
     * Safely selects the first matching element and returns the value of the
     * given attribute. Returns empty string if not found — never throws.
     */
    protected String safeAttr(Document doc, String cssSelector, String attr) {
        Element el = doc.selectFirst(cssSelector);
        return el != null ? cleanText(el.attr(attr)) : "";
    }

    /**
     * Parses GitHub-style abbreviated numbers: "12.5k" → 12500, "1.2m" → 1200000.
     * Falls back to plain integer parsing. Returns 0 if unparseable.
     */
    protected long parseAbbreviatedNumber(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        String cleaned = raw.trim().toLowerCase().replaceAll(",", "");
        try {
            if (cleaned.endsWith("k")) {
                return (long) (Double.parseDouble(cleaned.replace("k", "")) * 1_000);
            } else if (cleaned.endsWith("m")) {
                return (long) (Double.parseDouble(cleaned.replace("m", "")) * 1_000_000);
            }
            return Long.parseLong(cleaned.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // -------------------------------------------------------------------------
    // ParseResult — return type for all parsers
    // -------------------------------------------------------------------------

    /**
     * Container returned by every parse() call.
     * items    — TechTrend records extracted from this page.
     * nextUrls — follow-up URLs for CrawlerEngine to enqueue (pagination, filters).
     */
    public record ParseResult(List<TechTrend> items, List<String> nextUrls) {

        /** Convenience factory for an empty result (nothing extracted, no follow-ups). */
        public static ParseResult empty() {
            return new ParseResult(List.of(), List.of());
        }
    }
}
