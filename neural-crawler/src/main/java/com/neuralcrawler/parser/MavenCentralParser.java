package com.neuralcrawler.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralcrawler.model.TechCategory;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/*
  FILE: MavenCentralParser.java
  ===============================
  Parses Maven Central search API JSON responses — not HTML.
  HttpFetcherService fetches the JSON from the Solr search API endpoint,
  then this parser extracts library metadata into TechTrend records.

  API endpoint pattern:
    https://search.maven.org/solrsearch/select?q=<term>&rows=20&wt=json

  CONNECTS TO:
  - HtmlParserService defines the abstract parse() contract this implements.
    The "html" parameter here is actually the raw JSON string from the API.
  - CrawlerEngine calls parse() when the source is MAVEN_CENTRAL.
  - NormalizationService maps artifactIds to canonical tech names.
*/
@Component
public class MavenCentralParser extends HtmlParserService {

    private final ObjectMapper objectMapper;

    public MavenCentralParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param html      raw JSON string from the Maven Central Solr API (not HTML)
     * @param sourceUrl the API URL this response came from
     * @param snapshotId current snapshot ID
     */
    @Override
    public ParseResult parse(String html, String sourceUrl, String snapshotId) {
        if (html == null || html.isBlank()) return ParseResult.empty();

        List<TechTrend> items = new ArrayList<>();
        List<String> nextUrls = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(html);
            JsonNode docs = root.path("response").path("docs");

            if (!docs.isArray()) return ParseResult.empty();

            for (JsonNode doc : docs) {
                TechTrend trend = parseDoc(doc, snapshotId);
                if (trend != null) items.add(trend);
            }

            // Check if there are more pages — numFound vs current offset
            int numFound = root.path("response").path("numFound").asInt(0);
            int start = root.path("response").path("start").asInt(0);
            int rows = docs.size();
            if (start + rows < numFound && rows > 0) {
                // Build next page URL by incrementing start param
                String nextUrl = incrementStartParam(sourceUrl, start + rows);
                if (nextUrl != null) nextUrls.add(nextUrl);
            }

        } catch (Exception e) {
            // Malformed JSON — return empty rather than crashing the crawl
        }

        return new ParseResult(items, nextUrls);
    }

    private TechTrend parseDoc(JsonNode doc, String snapshotId) {
        String artifactId = doc.path("a").asText(null);
        String groupId = doc.path("g").asText(null);
        if (artifactId == null || groupId == null) return null;

        String latestVersion = doc.path("latestVersion").asText(null);
        int versionCount = doc.path("versionCount").asInt(0);
        long timestamp = doc.path("timestamp").asLong(0);

        String sourceUrl = "https://search.maven.org/artifact/" + groupId + "/" + artifactId;

        String description = latestVersion != null
                ? groupId + ":" + artifactId + " — latest: " + latestVersion
                        + " (" + versionCount + " versions)"
                : groupId + ":" + artifactId;

        TechTrend trend = TechTrend.of(artifactId, TrendSource.MAVEN_CENTRAL, snapshotId);
        trend.setSourceUrl(sourceUrl);
        trend.setCategory(TechCategory.LIBRARY);
        trend.setDescription(description);
        // versionCount used as a proxy for adoption signal
        trend.setDownloadCount(versionCount > 0 ? (long) versionCount : null);

        return trend;
    }

    private String incrementStartParam(String url, int newStart) {
        try {
            if (url.contains("start=")) {
                return url.replaceAll("start=\\d+", "start=" + newStart);
            } else {
                return url + "&start=" + newStart;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
