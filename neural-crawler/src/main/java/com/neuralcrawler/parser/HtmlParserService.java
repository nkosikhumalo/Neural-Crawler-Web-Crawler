
/*
  FILE: HtmlParserService.java
  ==============================
  The HTML parsing and data extraction layer — the equivalent of a Scrapy Spider's
  parse() callback method.

  WHAT IT DOES:
  - Accepts raw HTML string and the source URL, and uses Jsoup to parse it into a
    Document (DOM tree).
  - Applies CSS selectors to locate target elements:
      - ".product_pod h3 a"       → book title
      - ".price_color"            → price
      - ".instock.availability"   → stock status
      - "li.next a[href]"         → next page pagination link
  - Extracts text content and attributes (href, src) from matched elements and
    maps them into BookItem domain objects.
  - Discovers outbound links (anchor tags) within the page and filters them to
    stay within the same domain, returning them for the CrawlerEngine to enqueue.
  - Handles malformed or missing elements gracefully — a missing price element
    produces a null/empty field rather than throwing an exception.

  WHY IT EXISTS:
  Isolating parsing from fetching and orchestration means you can unit test extraction
  logic with a local HTML fixture file without making any network calls. It also makes
  adding a new site's selector configuration straightforward.

  CONNECTS TO:
  - CrawlerEngine calls parse(html, url) and gets back a ParseResult containing
    extracted BookItem objects and discovered links.
  - BookItem is the domain model that this parser populates.
  - SelectorConfig (from application.properties or a config object) provides the
    CSS selector strings so they're not hardcoded.
  - Jsoup library (declared in pom.xml) does all DOM work here.
*/
