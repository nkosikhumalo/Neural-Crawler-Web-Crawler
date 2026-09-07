
/*
  FILE: HackerNewsParser.java
  =============================
  Source-specific parser for news.ycombinator.com — extracts story titles, scores,
  and comment counts from Hacker News front page and "Ask HN" / "Show HN" posts.

  WHAT IT DOES:
  - Extends / implements HtmlParserService to fulfill the shared parse() contract.
  - Uses Jsoup CSS selectors targeting Hacker News's table-based DOM structure:
      - "tr.athing"              → each story row
      - "span.titleline a"       → story title and external URL
      - "span.sitestr"           → source domain of the linked article
      - "span.score"             → point/vote count (e.g., "342 points")
      - "a[href*=item]"          → comments link (also gives comment count)
  - Applies keyword detection on story titles to identify tech mentions:
      searches for known technology names, language names, and framework names
      within the title string to decide if a story is relevant to the radar.
  - Maps relevant stories into TechTrend with:
      - rawName derived from the detected tech keyword in the title
      - mentionCount set to the story's point score
      - source set to TrendSource.HACKERNEWS
      - sourceUrl set to the HN item URL
  - Handles pagination by detecting the "More" link at the bottom of the page
    and returning it as a follow-up URL if multi-page crawling is enabled.

  WHY IT EXISTS:
  Hacker News discussion velocity is a leading indicator — a tech getting heavily
  discussed on HN often precedes a spike in GitHub stars by days or weeks. Combining
  HN mentions with GitHub star data gives the radar a more complete signal.

  CONNECTS TO:
  - HtmlParserService defines the interface this implements.
  - CrawlerEngine calls this parser when the current URL matches news.ycombinator.com.
  - NormalizationService maps detected tech keywords to canonical names.
  - TechTrend is populated and returned for each relevant story found.
  - SelectorConfig holds the selector strings and the tech keyword detection list.
*/
