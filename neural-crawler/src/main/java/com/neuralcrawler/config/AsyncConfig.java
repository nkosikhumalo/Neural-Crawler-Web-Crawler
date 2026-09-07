
/*
  FILE: AsyncConfig.java
  ========================
  Spring configuration class that enables @Async execution and configures the
  thread pool used for parallel source crawl tasks and scheduled runs.

  WHAT IT DOES:
  - Annotated with @EnableAsync to activate Spring's @Async proxy infrastructure
    so any method annotated with @Async runs on a background thread rather than
    the caller's thread.
  - Also annotated with @EnableScheduling to activate @Scheduled method support,
    enabling SchedulerService's cron-triggered radar runs.
  - Defines a ThreadPoolTaskExecutor bean named "crawlerTaskExecutor" with:
      - corePoolSize: minimum threads always alive (set to number of sources, e.g., 3)
      - maxPoolSize: ceiling under burst load (e.g., 6)
      - queueCapacity: backlog buffer for queued tasks (e.g., 20)
      - threadNamePrefix: "radar-crawler-" so logs clearly identify background threads
      - waitForTasksToCompleteOnShutdown: true — ensures in-flight crawls finish
        cleanly rather than being interrupted when the app stops
      - awaitTerminationSeconds: 30 — max wait for shutdown completion

  WHY IT EXISTS:
  SchedulerService fires three parallel crawl tasks per snapshot run (one per source).
  Without a properly sized thread pool those tasks would queue and run serially,
  making each snapshot take 3x longer than necessary. This config is what enables
  true concurrent multi-source crawling.

  CONNECTS TO:
  - CrawlerService's @Async runSourceCrawl() method executes on the pool defined here.
  - SchedulerService's @Scheduled methods are activated by @EnableScheduling here.
  - Spring picks this up automatically through component scanning from NeuralCrawlerApplication.
  - application.properties can supply pool size values via @Value injection.
*/
