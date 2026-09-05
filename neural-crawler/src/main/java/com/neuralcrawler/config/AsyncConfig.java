
/*
  FILE: AsyncConfig.java
  ========================
  Spring configuration class that enables and customizes asynchronous method execution
  across the application.

  WHAT IT DOES:
  - Annotated with @EnableAsync to activate Spring's @Async infrastructure so methods
    annotated with @Async can be executed on background threads.
  - Defines a custom TaskExecutor bean (backed by a ThreadPoolTaskExecutor) with tuned
    core pool size, max pool size, and queue capacity appropriate for I/O-bound crawl tasks.
  - Configures the thread name prefix so logs clearly identify crawler background threads
    (e.g., "crawler-thread-1", "crawler-thread-2").

  WHY IT EXISTS:
  Web crawling is I/O-heavy and slow. Without async execution, a single HTTP request to
  a slow target site would block the entire web server thread, making the UI unresponsive.
  This class is what keeps the frontend snappy while crawls run in the background.

  CONNECTS TO:
  - CrawlerService uses @Async on its crawl-triggering methods — those run on the executor
    defined here.
  - CrawlerConfig may share or reference the thread pool settings to stay consistent.
  - Spring picks this up automatically through component scanning from NeuralCrawlerApplication.
*/
