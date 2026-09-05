
/*
  FILE: CrawlerConfig.java
  ==========================
  Spring configuration class that defines application-wide beans and settings
  for the crawling engine.

  WHAT IT DOES:
  - Declares and configures the ExecutorService / ThreadPoolExecutor bean that the
    crawler uses to run fetch-and-parse tasks concurrently across multiple threads.
  - Sets configurable properties like max thread count, request timeout, crawl depth
    limit, and politeness delay between requests (pulled from application.properties).
  - Registers an Apache HttpClient bean with shared connection pool settings, custom
    user-agent header, and redirect strategy so all crawler components reuse one client.

  WHY IT EXISTS:
  Keeping infrastructure setup out of business logic classes keeps them clean and testable.
  Thread pools and HTTP clients are expensive to create — they should be singletons shared
  across the app, which Spring bean lifecycle manages perfectly.

  CONNECTS TO:
  - CrawlerEngine injects the ExecutorService and HttpClient beans from here.
  - AsyncConfig works alongside this to enable @Async support on the service layer.
  - application.properties feeds the configurable values (depth, delay, thread count).
*/
