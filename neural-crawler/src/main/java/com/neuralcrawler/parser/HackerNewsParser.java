package com.neuralcrawler.parser;

import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
  FILE: HackerNewsParser.java
  =============================
  Parses news.ycombinator.com and extracts tech-related stories as TechTrend records.
  Uses keyword detection on story titles to identify relevant technology mentions.

  CONNECTS TO:
  - HtmlParserService defines the abstract parse() contract this implements.
  - SelectorConfig provides selectors and the tech keyword list.
  - CrawlerEngine calls parse() when the source is HACKERNEWS.
  - NormalizationService canonicalizes the raw keyword matched in the title.
*/
@Component
public class HackerNewsParser extends HtmlParserService {

    private static final String HN_BASE = "https://news.ycombinator.com";

    private final SelectorConfig selectors;

    public HackerNewsParser(SelectorConfig selectors) {
        this.selectors = selectors;
    }

    @Override
    public ParseResult parse(String html, String sourceUrl, String snapshotId) {
        if (html == null || html.isBlank()) return ParseResult.empty();

        Document doc = Jsoup.parse(html, sourceUrl);
        Elements storyRows = doc.select(selectors.getHnStoryRow());

        List<TechTrend> items = new ArrayList<>();
        List<String> nextUrls = new ArrayList<>();

        for (Element row : storyRows) {
            String storyId = row.id();
            // The score and metadata sit in the sibling row directly after
            Element metaRow = row.nextElementSibling();

            TechTrend trend = parseStory(row, metaRow, snapshotId);
            if (trend != null) items.add(trend);
        }

        // Pagination — "More" link
        Element more = doc.selectFirst(selectors.getHnNextPage());
        if (more != null) {
            String nextHref = more.attr("href");
            if (!nextHref.isBlank()) {
                nextUrls.add(nextHref.startsWith("http") ? nextHref : HN_BASE + "/" + nextHref);
            }
        }

        return new ParseResult(items, nextUrls);
    }

    private TechTrend parseStory(Element row, Element metaRow, String snapshotId) {
        // Title and URL
        Element titleEl = row.selectFirst(selectors.getHnStoryTitle());
        if (titleEl == null) return null;

        String title = cleanText(titleEl.text());
        String storyUrl = titleEl.attr("href");
        if (storyUrl.startsWith("item")) storyUrl = HN_BASE + "/" + storyUrl;

        // Detect a tech keyword in the title
        String matchedKeyword = detectKeyword(title);
        if (matchedKeyword == null) return null; // story not relevant to tech radar

        // Score — "342 points" → 342
        int score = 0;
        if (metaRow != null) {
            Element scoreEl = metaRow.selectFirst(selectors.getHnScore());
            if (scoreEl != null) {
                String scoreText = cleanText(scoreEl.text()).replaceAll("[^0-9]", "");
                try { score = Integer.parseInt(scoreText); } catch (NumberFormatException ignored) {}
            }
        }

        // HN item URL for the discussion thread
        String hnItemUrl = storyUrl;
        if (metaRow != null) {
            Element commentLink = metaRow.selectFirst(selectors.getHnCommentLink());
            if (commentLink != null) {
                String href = commentLink.attr("href");
                hnItemUrl = href.startsWith("http") ? href : HN_BASE + "/" + href;
            }
        }

        TechTrend trend = TechTrend.of(matchedKeyword, TrendSource.HACKERNEWS, snapshotId);
        trend.setSourceUrl(hnItemUrl);
        trend.setDescription(title.length() > 500 ? title.substring(0, 500) : title);
        trend.setMentionCount(score > 0 ? score : null);

        return trend;
    }

    /**
     * Scans the story title for any keyword from SelectorConfig.techKeywords.
     * Case-insensitive. Returns the first match found, or null if none.
     */
    private String detectKeyword(String title) {
        if (title == null) return null;
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        for (String keyword : selectors.getTechKeywords()) {
            if (lowerTitle.contains(keyword.toLowerCase(Locale.ROOT))) {
                return keyword;
            }
        }
        return null;
    }
}
