package com.neuralcrawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.parser.GitHubTrendingParser;
import com.neuralcrawler.parser.HackerNewsParser;
import com.neuralcrawler.parser.HtmlParserService;
import com.neuralcrawler.parser.MavenCentralParser;
import com.neuralcrawler.parser.SelectorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlParserServiceTest {

    private SelectorConfig selectorConfig;
    private GitHubTrendingParser githubParser;
    private HackerNewsParser hnParser;
    private MavenCentralParser mavenParser;
    private static final String SNAPSHOT_ID = "test-snapshot-001";

    @BeforeEach
    void setUp() {
        selectorConfig = new SelectorConfig();
        githubParser = new GitHubTrendingParser(selectorConfig);
        hnParser = new HackerNewsParser(selectorConfig);
        mavenParser = new MavenCentralParser(new ObjectMapper());
    }

    // -------------------------------------------------------------------------
    // GitHubTrendingParser tests
    // -------------------------------------------------------------------------

    @Test
    void githubParser_emptyHtml_returnsEmptyResult() {
        HtmlParserService.ParseResult result = githubParser.parse("", "https://github.com/trending", SNAPSHOT_ID);
        assertThat(result.items()).isEmpty();
        assertThat(result.nextUrls()).isEmpty();
    }

    @Test
    void githubParser_nullHtml_returnsEmptyResult() {
        HtmlParserService.ParseResult result = githubParser.parse(null, "https://github.com/trending", SNAPSHOT_ID);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void githubParser_validHtml_extractsRepositories() {
        String html = buildGithubHtml("rust-lang", "rust", "A language empowering everyone.", "Rust", "85,432", "rust");
        HtmlParserService.ParseResult result = githubParser.parse(html, "https://github.com/trending", SNAPSHOT_ID);

        assertThat(result.items()).isNotEmpty();
        TechTrend trend = result.items().get(0);
        assertThat(trend.getRawName()).isEqualToIgnoringCase("rust");
        assertThat(trend.getStarCount()).isGreaterThan(0);
        assertThat(trend.getSnapshotId()).isEqualTo(SNAPSHOT_ID);
    }

    @Test
    void githubParser_abbreviatedStarCount_parsesCorrectly() {
        String html = buildGithubHtml("owner", "myrepo", "desc", "Go", "12.5k", "go");
        HtmlParserService.ParseResult result = githubParser.parse(html, "https://github.com/trending", SNAPSHOT_ID);

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items().get(0).getStarCount()).isEqualTo(12_500L);
    }

    // -------------------------------------------------------------------------
    // HackerNewsParser tests
    // -------------------------------------------------------------------------

    @Test
    void hnParser_emptyHtml_returnsEmptyResult() {
        HtmlParserService.ParseResult result = hnParser.parse("", "https://news.ycombinator.com", SNAPSHOT_ID);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void hnParser_storyWithNoKeyword_isExcluded() {
        String html = buildHnHtml("Breaking: Local Cat Found On Roof", "342");
        HtmlParserService.ParseResult result = hnParser.parse(html, "https://news.ycombinator.com", SNAPSHOT_ID);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void hnParser_storyWithKeyword_isIncluded() {
        String html = buildHnHtml("Why Rust is taking over systems programming in 2024", "450");
        HtmlParserService.ParseResult result = hnParser.parse(html, "https://news.ycombinator.com", SNAPSHOT_ID);
        assertThat(result.items()).isNotEmpty();
        assertThat(result.items().get(0).getMentionCount()).isEqualTo(450);
    }

    // -------------------------------------------------------------------------
    // MavenCentralParser tests
    // -------------------------------------------------------------------------

    @Test
    void mavenParser_validJson_extractsArtifacts() {
        String json = "{"
                + "\"response\":{"
                + "\"numFound\":1,\"start\":0,"
                + "\"docs\":[{"
                + "\"a\":\"spring-boot\","
                + "\"g\":\"org.springframework.boot\","
                + "\"latestVersion\":\"3.3.0\","
                + "\"versionCount\":120"
                + "}]}}";

        HtmlParserService.ParseResult result = mavenParser.parse(json, "https://search.maven.org", SNAPSHOT_ID);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getRawName()).isEqualTo("spring-boot");
    }

    @Test
    void mavenParser_malformedJson_returnsEmpty() {
        HtmlParserService.ParseResult result = mavenParser.parse("{not-valid-json}", "https://search.maven.org", SNAPSHOT_ID);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void mavenParser_missingFields_handledGracefully() {
        String json = "{\"response\":{\"numFound\":1,\"start\":0,\"docs\":[{\"a\":\"mylib\",\"g\":\"com.example\"}]}}";
        HtmlParserService.ParseResult result = mavenParser.parse(json, "https://search.maven.org", SNAPSHOT_ID);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getDownloadCount()).isNull();
    }

    // -------------------------------------------------------------------------
    // HTML fixture builders
    // -------------------------------------------------------------------------

    private String buildGithubHtml(String owner, String repo, String desc, String lang, String stars, String tag) {
        return "<html><body>"
                + "<article class=\"Box-row\">"
                + "<h2 class=\"h3\"><a href=\"/" + owner + "/" + repo + "\">" + owner + " / " + repo + "</a></h2>"
                + "<p class=\"col-9\">" + desc + "</p>"
                + "<span itemprop=\"programmingLanguage\">" + lang + "</span>"
                + "<a class=\"Link--muted\"><svg class=\"octicon-star\"></svg>" + stars + "</a>"
                + "<a class=\"topic-tag\">" + tag + "</a>"
                + "</article>"
                + "</body></html>";
    }

    private String buildHnHtml(String title, String score) {
        return "<html><body><table>"
                + "<tr class=\"athing\" id=\"12345\">"
                + "<td><span class=\"titleline\"><a href=\"https://example.com\">" + title + "</a></span></td>"
                + "</tr>"
                + "<tr>"
                + "<td><span class=\"score\">" + score + " points</span>"
                + "<a href=\"item?id=12345\">100 comments</a></td>"
                + "</tr>"
                + "</table></body></html>";
    }
}
