package com.neuralcrawler.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralcrawler.model.TechCategory;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendSource;
import com.neuralcrawler.util.RequestSecurityUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/*
  FILE: StackOverflowJobsParser.java
  ====================================
  Parses the Stack Exchange Tags API response to extract the most popular
  technology tags on Stack Overflow — a strong signal of developer mindshare.

  FREE API used (no auth, no key required for basic access):
    GET https://api.stackexchange.com/2.3/tags?order=desc&sort=popular&site=stackoverflow&pagesize=50&filter=default

  Returns JSON with tag names and their question counts.
  Tag question count = how many SO questions use this tag = developer adoption signal.

  CONNECTS TO:
  - HtmlParserService defines the abstract parse() contract (json string as "html" param).
  - CrawlerEngine calls parse() when source is STACKOVERFLOW_JOBS.
  - TrendSource.STACKOVERFLOW_JOBS tags all records from this parser.
*/
@Component
public class StackOverflowJobsParser extends HtmlParserService {

    private final ObjectMapper objectMapper;
    private final RequestSecurityUtils security;

    public StackOverflowJobsParser(ObjectMapper objectMapper, RequestSecurityUtils security) {
        this.objectMapper = objectMapper;
        this.security = security;
    }

    @Override
    public ParseResult parse(String json, String sourceUrl, String snapshotId) {
        if (json == null || json.isBlank()) return ParseResult.empty();

        List<TechTrend> items = new ArrayList<>();
        List<String> nextUrls = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode tags = root.path("items");

            if (!tags.isArray()) return ParseResult.empty();

            for (JsonNode tag : tags) {
                String name = security.sanitize(tag.path("name").asText(""));
                int count   = tag.path("count").asInt(0);

                if (name.isBlank() || count == 0) continue;

                TechTrend trend = TechTrend.of(name, TrendSource.STACKOVERFLOW_JOBS, snapshotId);
                trend.setCategory(TechCategory.TOOL);
                trend.setMentionCount(count);
                trend.setSourceUrl("https://stackoverflow.com/questions/tagged/" + name);
                trend.setDescription(count + " questions on Stack Overflow");
                items.add(trend);
            }

            // Handle pagination
            boolean hasMore = root.path("has_more").asBoolean(false);
            if (hasMore && !items.isEmpty()) {
                String nextPage = incrementPage(sourceUrl);
                if (nextPage != null) nextUrls.add(nextPage);
            }

        } catch (Exception e) {
            // Malformed JSON — return what we have
        }

        return new ParseResult(items, nextUrls);
    }

    private String incrementPage(String url) {
        try {
            if (url.contains("page=")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("page=(\\d+)").matcher(url);
                if (m.find()) {
                    int current = Integer.parseInt(m.group(1));
                    return url.substring(0, m.start()) + "page=" + (current + 1) + url.substring(m.end());
                }
            }
            return url + "&page=2";
        } catch (Exception e) {
            return null;
        }
    }
}
