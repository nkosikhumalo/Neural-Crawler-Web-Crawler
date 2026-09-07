
/*
  FILE: HtmlParserService.java
  ==============================
  Base interface / abstract class defining the parsing contract that all
  source-specific parsers must implement.

  WHAT IT DOES:
  - Defines the shared parse(html, sourceUrl) method signature that every
    source-specific parser (GitHubTrendingParser, HackerNewsParser,
    MavenCentralParser) implements.
  - Provides shared utility methods used by all parsers:
      - cleanText(raw)         — strips whitespace, non-breaking spaces, and HTML entities
      - resolveAbsoluteUrl(href, base) — converts relative hrefs to absolute URLs
      - safeSelect(doc, selector) — wraps Jsoup select() calls with null-safe fallback
        so a missing element returns empty string rather than throwing NullPointerException
  - Defines the return type: ParseResult — a container holding a List<TechTrend>
    (extracted items) and a List<String> (discovered follow-up URLs for the engine).

  WHY IT EXISTS:
  With three different source parsers that all need the same utility methods and
  the same return contract, a shared base prevents code duplication and ensures
  CrawlerEngine can call any parser through a single consistent interface without
  knowing which source it's working with.

  CONNECTS TO:
  - GitHubTrendingParser, HackerNewsParser, MavenCentralParser all extend/implement this.
  - CrawlerEngine calls the parse() method defined here on whichever parser matches
    the current source being crawled.
  - TechTrend is the domain object all parsers populate and return.
  - Jsoup Document is the input type — produced by Jsoup.parse(html) in each subclass.
*/
