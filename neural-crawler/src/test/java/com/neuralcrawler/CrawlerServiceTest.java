
/*
  FILE: CrawlerServiceTest.java
  ===============================
  Integration/unit test class for CrawlerService — verifies crawl orchestration
  logic using mocked dependencies.

  WHAT IT TESTS:
  - That startCrawl() initializes a CrawlJob with PENDING status and transitions it
    to RUNNING when the async method executes.
  - That getStatus() returns the correct CrawlJob state at each stage.
  - That when HttpFetcherService (mocked) returns HTML, CrawlerService coordinates
    parser and repository calls correctly.
  - That cancelCrawl() sets the stop signal and the crawl terminates gracefully.
  - That errors thrown by HttpFetcherService are caught and set the CrawlJob to FAILED
    with an appropriate error message.

  WHY IT EXISTS:
  CrawlerService coordinates multiple collaborators — testing it with mocks isolates
  the orchestration logic from real HTTP calls, database writes, and timing issues.
  It validates that the business workflow behaves correctly in isolation.

  CONNECTS TO:
  - CrawlerService is the class under test.
  - Mockito is used to mock CrawlerEngine, HtmlParserService, and CrawlResultRepository.
  - JUnit 5 + Spring Boot Test provide the test runner and context.
  - @MockBean annotations replace real beans in the Spring context with mocks.
*/
