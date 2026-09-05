
/*
  FILE: BookItem.java
  =====================
  Domain model representing a single scraped book record extracted from the target website.

  WHAT IT DOES:
  - Holds the structured data fields for one book:
      - title        (String)  — the book's full title
      - price        (String)  — raw price text as scraped (e.g., "£51.77")
      - availability (String)  — stock status text (e.g., "In stock")
      - rating       (String)  — star rating extracted from CSS class names
      - url          (String)  — the absolute URL of this book's detail page
      - imageUrl     (String)  — the book cover image src attribute
      - scrapedAt    (LocalDateTime) — timestamp of when this record was extracted
  - Simple POJO with getters, setters, and a no-arg constructor.
  - Annotated for JSON serialization (Jackson) and optionally JPA persistence if a
    database is wired up.

  WHY IT EXISTS:
  A typed domain model makes the data flow explicit and type-safe throughout the system.
  It's the central data contract between the parser (which creates it), the DAO (which
  stores it), the export service (which serializes it), and the frontend (which displays it).

  CONNECTS TO:
  - HtmlParserService creates instances of this class after extracting data.
  - CrawlResultRepository stores and retrieves lists of BookItem.
  - ExportService serializes BookItem lists to CSV/JSON.
  - CrawlResultDTO wraps or maps from BookItem to send data to the frontend.
*/
