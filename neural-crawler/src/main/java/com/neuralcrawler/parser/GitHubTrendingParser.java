
/*
  FILE: GitHubTrendingParser.java
  =================================
  Source-specific parser for github.com/trending — extracts trending repositories
  and their associated metrics from GitHub's trending page HTML.

  WHAT IT DOES:
  - Extends / implements HtmlParserService to fulfill the shared parse() contract.
  - Uses Jsoup CSS selectors targeting GitHub Trending's DOM structure:
      - "article.Box-row"              → each repository card container
      - "h2.h3 a"                      → repository name and owner (e.g., "rust-lang/rust")
      - "p.col-9"                      → repository description text
      - "span[itemprop=programmingLanguage]" → primary programming language
      - "a.Link--muted svg.octicon-star" parent → star count text
      - "a.Link--muted svg.octicon-repo-forked" parent → fork count text
      - "span.d-inline-block.float-sm-right" → stars gained today/this week
      - "a.topic-tag"                  → repository topic tags
  - Maps each repository card into a TechTrend with:
      - rawName set to the repository name
      - starCount set to the total star count (parsed from "12.5k" style strings)
      - tags set to the list of topic tags for NormalizationService to process
      - source set to TrendSource.GITHUB
  - Handles GitHub's "k" and "m" number abbreviations (e.g., "12.5k" → 12500).
  - Discovers the language filter links at the top of the page and optionally returns
    them as follow-up URLs if multi-language crawling is configured.

  WHY IT EXISTS:
  GitHub Trending is the highest-signal source in the radar — star velocity here is
  the clearest indicator of what the developer community is currently excited about.
  A dedicated parser keeps GitHub-specific selector logic isolated and independently
  testable with a saved GitHub HTML fixture.

  CONNECTS TO:
  - HtmlParserService defines the interface this implements.
  - CrawlerEngine calls this parser when the current URL matches github.com/trending.
  - NormalizationService processes the raw tags extracted here into canonical names.
  - TechTrend is populated and returned from each parsed repository card.
  - SelectorConfig holds the CSS selector strings (externalized from this file).
*/
