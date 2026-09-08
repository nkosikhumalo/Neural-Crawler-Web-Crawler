package com.neuralcrawler.parser;

import com.neuralcrawler.model.TechCategory;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/*
  FILE: GitHubTrendingParser.java
  =================================
  Parses github.com/trending pages and extracts repository data into TechTrend records.

  CONNECTS TO:
  - HtmlParserService defines the abstract parse() contract this implements.
  - SelectorConfig provides the CSS selector strings.
  - CrawlerEngine calls parse() when the source is GITHUB.
  - NormalizationService processes the rawName and tags after this returns.
*/
@Component
public class GitHubTrendingParser extends HtmlParserService {

    private final SelectorConfig selectors;

    public GitHubTrendingParser(SelectorConfig selectors) {
        this.selectors = selectors;
    }

    @Override
    public ParseResult parse(String html, String sourceUrl, String snapshotId) {
        if (html == null || html.isBlank()) return ParseResult.empty();

        Document doc = Jsoup.parse(html, sourceUrl);
        Elements cards = doc.select(selectors.getGithubCard());

        List<TechTrend> items = new ArrayList<>();
        List<String> nextUrls = new ArrayList<>();

        for (Element card : cards) {
            TechTrend trend = parseCard(card, sourceUrl, snapshotId);
            if (trend != null) items.add(trend);
        }

        // Discover language filter links as follow-up URLs
        doc.select("a.filter-item[href*=/trending/]").forEach(a -> {
            String href = a.absUrl("href");
            if (!href.isBlank()) nextUrls.add(href);
        });

        return new ParseResult(items, nextUrls);
    }

    private TechTrend parseCard(Element card, String sourceUrl, String snapshotId) {
        // Repository name — format is "owner / repo"
        Element nameEl = card.selectFirst(selectors.getGithubRepoName());
        if (nameEl == null) return null;

        String repoPath = cleanText(nameEl.attr("href")).replaceFirst("^/", "");
        String repoName = repoPath.contains("/") ? repoPath.split("/")[1] : repoPath;

        // Description
        Element descEl = card.selectFirst(selectors.getGithubDescription());
        String description = descEl != null ? cleanText(descEl.text()) : "";

        // Primary language
        Element langEl = card.selectFirst(selectors.getGithubLanguage());
        String language = langEl != null ? cleanText(langEl.text()) : "";

        // Star count — text like "12,345" or "1.2k"
        Element starEl = card.selectFirst(selectors.getGithubStarCount());
        long stars = starEl != null ? parseAbbreviatedNumber(starEl.text()) : 0L;

        // Topic tags
        List<String> tags = new ArrayList<>();
        card.select(selectors.getGithubTopicTag()).forEach(t -> tags.add(cleanText(t.text())));

        // Add language as a tag too if present
        if (!language.isBlank() && !tags.contains(language)) {
            tags.add(0, language);
        }

        String fullUrl = "https://github.com/" + repoPath;

        TechTrend trend = TechTrend.of(repoName, TrendSource.GITHUB, snapshotId);
        trend.setSourceUrl(fullUrl);
        trend.setDescription(description.isBlank() ? null : description);
        trend.setStarCount(stars > 0 ? stars : null);
        trend.setTags(tags);

        // Derive category from language if available
        if (!language.isBlank()) {
            trend.setCategory(TechCategory.LANGUAGE);
        }

        return trend;
    }
}
