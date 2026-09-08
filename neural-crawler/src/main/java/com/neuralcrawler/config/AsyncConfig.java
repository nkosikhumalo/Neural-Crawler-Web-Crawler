package com.neuralcrawler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/*
  FILE: AsyncConfig.java
  ========================
  Enables @Async and @Scheduled support, and defines the task executor that
  runs CrawlerService's async source crawl methods in parallel.

  CONNECTS TO:
  - CrawlerService.runSourceCrawl() is @Async and runs on crawlerTaskExecutor.
  - SchedulerService's @Scheduled methods are activated by @EnableScheduling here.
  - application.properties supplies pool sizing values.
*/
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Value("${crawler.thread-pool.size:4}")
    private int corePoolSize;

    /**
     * Task executor for @Async crawler methods.
     * Named "crawlerTaskExecutor" so @Async methods can reference it explicitly
     * via @Async("crawlerTaskExecutor") to avoid using the default executor.
     */
    @Bean(name = "crawlerTaskExecutor")
    public Executor crawlerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(corePoolSize, 3));
        executor.setMaxPoolSize(Math.max(corePoolSize * 2, 6));
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("radar-crawler-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
