
/*
  FILE: UrlUtils.java
  =====================
  Stateless utility class providing URL manipulation and validation helper methods
  used across the crawler.

  WHAT IT DOES:
  - normalizeUrl(url): strips trailing slashes, lowercases the scheme and host,
    removes fragment identifiers (#section), and resolves relative URLs against a
    base URL to produce a consistent absolute URL.
  - isSameDomain(url, baseUrl): checks whether a discovered link belongs to the same
    domain as the seed URL to prevent the crawler from wandering off-site.
  - isValidUrl(url): validates that a string is a well-formed HTTP/HTTPS URL and not
    a mailto:, javascript:, or other non-crawlable scheme.
  - extractDomain(url): returns just the hostname from a full URL string.

  WHY IT EXISTS:
  URL handling edge cases are surprisingly tricky — relative links, protocol-relative
  URLs, query strings, and fragments all need consistent treatment. Centralizing this
  logic prevents bugs scattered across CrawlerEngine and HtmlParserService.

  CONNECTS TO:
  - CrawlerEngine calls isSameDomain() and normalizeUrl() before enqueuing any link.
  - HtmlParserService calls normalizeUrl() when resolving relative href attributes.
  - RobotsService uses extractDomain() to know which robots.txt to fetch.
*/
