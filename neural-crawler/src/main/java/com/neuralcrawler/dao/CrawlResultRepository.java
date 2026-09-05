
/*
  FILE: CrawlResultRepository.java
  ===================================
  Data Access Object (DAO) / Repository layer responsible for storing and retrieving
  extracted crawl data.

  WHAT IT DOES:
  - In its simplest form: an in-memory store using a thread-safe ConcurrentHashMap
    keyed by jobId, holding a List<BookItem> per crawl session. This requires no
    database setup and works fine for a single-node app.
  - In its full form: a Spring Data JPA repository interface (extends JpaRepository)
    that persists BookItem entities to a relational database (H2 for dev, PostgreSQL
    for production), with methods like findByJobId(), deleteByJobId(), countByJobId().
  - Exposes CRUD methods the service layer calls: save(item), findAllByJobId(id),
    clearResults(jobId), getLatestResults().

  WHY IT EXISTS:
  Abstracting storage behind a repository interface means the rest of the app doesn't
  care whether data lives in memory, H2, or PostgreSQL. Swapping storage backends only
  requires changing this class and its configuration.

  CONNECTS TO:
  - CrawlerService / HtmlParserService saves BookItem objects here as they're extracted.
  - CrawlController reads results from here via CrawlerService.
  - ExportService reads from here to get all items for CSV/JSON export.
  - BookItem is the entity/model this repository manages.
*/
