
/*
  FILE: MavenCentralParser.java
  ===============================
  Source-specific parser for search.maven.org — extracts library popularity signals
  from Maven Central's search results and library detail pages.

  WHAT IT DOES:
  - Extends / implements HtmlParserService to fulfill the shared parse() contract.
  - Maven Central's search UI is JavaScript-rendered (Angular-based), so this parser
    works with the Maven Central REST search API instead of raw HTML scraping:
        GET https://search.maven.org/solrsearch/select?q=<term>&rows=20&wt=json
    The API returns JSON directly, bypassing the need for Playwright or Jsoup for
    the main data. Jsoup is still used for any supplementary HTML page parsing.
  - Parses the JSON API response to extract:
      - artifactId / groupId      → library name (e.g., "spring-boot", "org.springframework")
      - latestVersion             → most recently released version string
      - versionCount              → total number of published versions (signals maturity)
      - timestamp                 → date of the latest release
      - repositoryId              → "central" or other repo identifier
  - Maps each library result into a TechTrend with:
      - rawName set to artifactId
      - downloadCount set to a usage popularity score if available
      - source set to TrendSource.MAVEN_CENTRAL
      - category set to TechCategory.LIBRARY

  WHY IT EXISTS:
  Maven Central adoption data represents actual production usage in the Java/JVM
  ecosystem — a library being widely depended upon is a strong signal of real-world
  traction beyond just GitHub stars or developer discussion.

  CONNECTS TO:
  - HtmlParserService defines the interface this implements.
  - HttpFetcherService fetches the Maven Central API JSON response.
  - Jackson ObjectMapper (or Jsoup for any HTML portions) parses the response.
  - CrawlerEngine routes Maven Central URLs to this parser.
  - NormalizationService maps artifactIds to canonical tech names where applicable.
  - TechTrend is populated and returned for each library result.
*/
