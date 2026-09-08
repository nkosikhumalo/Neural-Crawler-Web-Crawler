package com.neuralcrawler.crawler;

import com.neuralcrawler.model.CrawlJob;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendSource;
import com.neuralcrawler.parser.HtmlParserService;
import com.neuralcrawler.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/*
  FILE: CrawlerEngine.java
  ==========================
  Manages the URL frontier and fetch loop for a single source crawl job.
  Each call to crawl() uses its own stop flag so parallel source jobs
  (GitHub, HN, Maven running concurrently) never interfere with each other.
*/
@Component
public class CrawlerEngine {

    private static final Logger log = LoggerFactory.getLogger(CrawlerEngine.class);

    @Value("${crawler.max-pages-per-source:20}")
    private int maxPagesPerSource;

    private final HttpFetcherService httpFetcher;
    private final PlaywrightFetcherService playwrightFetcher;
    private final RobotsService robotsService;
    private final Map<TrendSource, HtmlParserService> parsers;

    // Per-job stop flags keyed by jobId — no shared mutable state across jobs
    private final ConcurrentHashMap<String, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();

    public CrawlerEngine(HttpFetcherService httpFetcher,
                         PlaywrightFetcherService playwrightFetcher,
                         RobotsService robotsService,
                         com.neuralcrawler.parser.GitHubTrendingParser githubParser,
                         com.neuralcrawler.parser.HackerNewsParser hnParser,
                         com.neuralcrawler.parser.HackerNewsApiParser hnApiParser,
                         com.neuralcrawler.parser.MavenCentralParser mavenParser,
                         com.neuralcrawler.parser.StackOverflowJobsParser soParser) {
        this.httpFetcher = httpFetcher;
        this.playwrightFetcher = playwrightFetcher;
        this.robotsService = robotsService;
        this.parsers = Map.of(
                TrendSource.GITHUB, githubParser,
                TrendSource.HACKERNEWS, hnParser,
                TrendSource.HACKERNEWS_API, hnApiParser,
                TrendSource.MAVEN_CENTRAL, mavenParser,
                TrendSource.STACKOVERFLOW_JOBS, soParser
        );
    }

    /**
     * Runs the full crawl for the given job. Each invocation registers its own
     * AtomicBoolean stop flag so concurrent jobs don't share cancellation state.
     */
    public List<TechTrend> crawl(CrawlJob job, List<String> seedUrls, boolean usePlaywright) {
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        stopFlags.put(job.getJobId(), stopFlag);

        try {
            return doCrawl(job, seedUrls, usePlaywright, stopFlag);
        } finally {
            stopFlags.remove(job.getJobId());
        }
    }

    private List<TechTrend> doCrawl(CrawlJob job, List<String> seedUrls,
                                     boolean usePlaywright, AtomicBoolean stop) {
        Queue<String> frontier = new ConcurrentLinkedQueue<>(seedUrls);
        Set<String> visited = ConcurrentHashMap.newKeySet();
        List<TechTrend> allItems = Collections.synchronizedList(new ArrayList<>());

        HtmlParserService parser = parsers.get(job.getSource());
        if (parser == null) {
            log.error("No parser for source: {}", job.getSource());
            return allItems;
        }

        int pages = 0;

        while (!frontier.isEmpty() && !stop.get() && pages < maxPagesPerSource) {
            String url = frontier.poll();
            if (url == null || visited.contains(url)) continue;
            if (!UrlUtils.isValidCrawlUrl(url)) continue;
            if (!isApiSource(job.getSource()) && !robotsService.isAllowed(url)) {
                log.debug("Blocked by robots.txt: {}", url);
                continue;
            }

            visited.add(url);

            String content = usePlaywright
                    ? playwrightFetcher.fetch(url)
                    : httpFetcher.fetch(url);

            if (content == null) continue;

            HtmlParserService.ParseResult result = parser.parse(content, url, job.getSnapshotId());
            allItems.addAll(result.items());

            result.nextUrls().stream()
                    .filter(u -> !visited.contains(u))
                    .filter(UrlUtils::isValidCrawlUrl)
                    .forEach(frontier::add);

            pages++;
            job.recordProgress(result.items().size());

            log.debug("[{}] {} — {} items, {} pages done", job.getSource(), url, result.items().size(), pages);
        }

        log.info("[{}] Done. Pages: {}, Items: {}", job.getSource(), pages, allItems.size());
        return allItems;
    }

    private boolean isApiSource(TrendSource source) {
        return source == TrendSource.HACKERNEWS_API
                || source == TrendSource.MAVEN_CENTRAL
                || source == TrendSource.STACKOVERFLOW_JOBS;
    }

    /** Signals a specific job's crawl to stop cleanly. */
    public void stop(String jobId) {
        AtomicBoolean flag = stopFlags.get(jobId);
        if (flag != null) flag.set(true);
    }
}
