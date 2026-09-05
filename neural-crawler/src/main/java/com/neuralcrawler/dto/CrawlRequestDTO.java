
/*
  FILE: CrawlRequestDTO.java
  ============================
  Data Transfer Object that represents the incoming crawl request payload sent
  from the frontend to the POST /api/crawl endpoint.

  WHAT IT DOES:
  - Defines the shape of the JSON body the frontend submits:
      - targetUrl    (String)        — the seed URL to start crawling from (required)
      - maxDepth     (int)           — how many link-levels deep to crawl (default: 2)
      - maxPages     (int)           — hard cap on total pages to visit
      - delay        (int)           — milliseconds to wait between requests
      - selectorConfig (SelectorConfig) — optional custom CSS selectors for the target site
  - Annotated with @Valid / Bean Validation annotations (@NotBlank, @Min, @Max) so
    Spring automatically validates the payload before it reaches the service.

  WHY IT EXISTS:
  DTOs decouple the HTTP API contract from internal domain models. The frontend can
  change what fields it sends without directly touching domain/model classes, and
  validation annotations here keep dirty data out of the service layer.

  CONNECTS TO:
  - CrawlController deserializes the HTTP request body into this DTO.
  - CrawlerService receives this DTO and extracts config values from it.
  - SelectorConfig is a nested or referenced object within this DTO.
*/
