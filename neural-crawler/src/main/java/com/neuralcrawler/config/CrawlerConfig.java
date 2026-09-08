package com.neuralcrawler.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
  FILE: CrawlerConfig.java
  ==========================
  Defines the shared infrastructure beans — HttpClient and ExecutorService —
  that the crawler layer injects. Singletons managed by Spring.

  CONNECTS TO:
  - HttpFetcherService injects the CloseableHttpClient bean.
  - CrawlerEngine injects the ExecutorService bean.
  - AsyncConfig provides the @Async thread pool separately for service-layer async.
  - application.properties supplies all @Value properties.
*/
@Configuration
public class CrawlerConfig {

    @Value("${crawler.thread-pool.size:4}")
    private int threadPoolSize;

    @Value("${crawler.http.connect-timeout:5000}")
    private int connectTimeoutMs;

    @Value("${crawler.http.read-timeout:10000}")
    private int readTimeoutMs;

    @Value("${crawler.user-agent:NeuralCrawler/1.0 (Tech Stack Radar)}")
    private String userAgent;

    /**
     * Shared Apache HttpClient 5 with connection pooling, timeouts, and a
     * custom User-Agent. Reused across all fetch requests in the application.
     */
    @Bean
    public CloseableHttpClient httpClient() {
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(20);
        connManager.setDefaultMaxPerRoute(5);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent(userAgent)
                .disableRedirectHandling()
                .build();
    }

    /**
     * Fixed thread pool for running concurrent per-source crawl tasks.
     * Minimum 3 threads — one per source (GitHub, HackerNews, Maven Central).
     */
    @Bean
    public ExecutorService crawlerExecutorService() {
        return Executors.newFixedThreadPool(Math.max(threadPoolSize, 3));
    }
}
