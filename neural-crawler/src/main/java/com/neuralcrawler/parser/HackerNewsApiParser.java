package com.neuralcrawler.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendSource;
import com.neuralcrawler.util.RequestSecurityUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
  FILE: HackerNewsApiParser.java
  ================================
  Parses the official HN Firebase REST API (free, no auth, no scraping).
  Uses HACKERNEWS_API as its TrendSource.

  API used:
  - Top stories list:  https://hacker-news.firebaseio.com/v0/topstories.json
  - Story detail:      https://hacker-news.firebaseio.com/v0/item/{id}.json
  - Ask HN stories:    https://hacker-news.firebaseio.com/v0/askstories.json
  - Show HN stories:   https://hacker-news.firebaseio.com/v0/showstories.json

  The "html" parameter received by parse() is the raw JSON from one of the above.
  The CrawlerEngine feeds individual story JSON URLs as follow-up URLs so each
  story's detail is fetched and parsed in subsequent engine iterations.

  CONNECTS TO:
  - HtmlParserService defines the abstract parse() contract.
  - CrawlerEngine calls parse() when source is HACKERNEWS_API.
  - SelectorConfig.techKeywords used for keyword detection on story titles.
  - RequestSecurityUtils.sanitize() cleans all text before storage.
*/
@Component
public class HackerNewsApiParser extends HtmlParserService {

    private static final String HN_API_BASE = "https://hacker-news.firebaseio.com/v0";
    private static final int MAX_STORIES_TO_FETCH = 50;

    private final ObjectMapper objectMapper;
    private final SelectorConfig selectors;
    private final RequestSecurityUtils security;

    public HackerNewsApiParser(ObjectMapper objectMapper,
                                SelectorConfig selectors,
                                RequestSecurityUtils security) {
        this.objectMapper = objectMapper;
        this.selectors = selectors;
        this.security = security;
    }

    @Override
    public ParseResult parse(String json, String sourceUrl, String snapshotId) {
        if (json == null || json.isBlank()) return ParseResult.empty();

        List<TechTrend> items = new ArrayList<>();
        List<String> nextUrls = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);

            // Case 1: array of story IDs from topstories.json / askstories.json
            if (root.isArray()) {
                int count = 0;
                for (JsonNode idNode : root) {
                    if (count++ >= MAX_STORIES_TO_FETCH) break;
                    nextUrls.add(HN_API_BASE + "/item/" + idNode.asLong() + ".json");
                }
                return new ParseResult(items, nextUrls);
            }

            // Case 2: individual story object from item/{id}.json
            if (root.isObject()) {
                TechTrend trend = parseStoryObject(root, snapshotId);
                if (trend != null) items.add(trend);
            }

        } catch (Exception e) {
            // Malformed JSON — skip silently
        }

        return new ParseResult(items, nextUrls);
    }

    private TechTrend parseStoryObject(JsonNode story, String snapshotId) {
        String type = story.path("type").asText("");
        // Only process stories (not comments, polls, etc.)
        if (!type.equals("story")) return null;

        String title = security.sanitize(story.path("title").asText(""));
        if (title.isBlank()) return null;

        // Detect tech keyword
        String keyword = detectKeyword(title);
        String trendName = keyword != null ? keyword : title;

        int score = story.path("score").asInt(0);
        int comments = story.path("descendants").asInt(0);
        long id = story.path("id").asLong(0);
        String url = security.sanitize(story.path("url").asText(""));
        String by = security.sanitize(story.path("by").asText("anonymous"));

        String hnItemUrl = "https://news.ycombinator.com/item?id=" + id;
        String description = title + " | by " + by + " | " + comments + " comments";

        TechTrend trend = TechTrend.of(trendName, TrendSource.HACKERNEWS_API, snapshotId);
        if (keyword == null) trend.setCategory(com.neuralcrawler.model.TechCategory.OTHER);
        trend.setSourceUrl(hnItemUrl);
        trend.setDescription(security.sanitize(description));
        trend.setMentionCount(score > 0 ? score : null);

        return trend;
    }

    private String detectKeyword(String title) {
        if (title == null) return null;
        String lower = title.toLowerCase(Locale.ROOT);
        for (String keyword : selectors.getTechKeywords()) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return keyword;
            }
        }
        return null;
    }
}
