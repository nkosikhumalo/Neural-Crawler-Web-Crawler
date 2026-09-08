package com.neuralcrawler.service;

import com.neuralcrawler.crawler.CrawlerEngine;
import com.neuralcrawler.dao.CrawlResultRepository;
import com.neuralcrawler.model.CrawlJob;
import com.neuralcrawler.model.TechTrend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    @Value("${radar.sources.github.use-playwright:false}")
    private boolean githubUsePlaywright;

    private final CrawlerEngine engine;
    private final NormalizationService normalizationService;
    private final CrawlResultRepository repository;

    public CrawlerService(CrawlerEngine engine,
                          NormalizationService normalizationService,
                          CrawlResultRepository repository) {
        this.engine = engine;
        this.normalizationService = normalizationService;
        this.repository = repository;
    }

    @Async("crawlerTaskExecutor")
    public CompletableFuture<CrawlJob> runSourceCrawl(CrawlJob job, List<String> seedUrls) {
        job.start();
        log.info("Starting crawl job {} for source {}", job.getJobId(), job.getSource());

        try {
            boolean usePlaywright = job.getSource().name().equals("GITHUB") && githubUsePlaywright;
            List<TechTrend> items = engine.crawl(job, seedUrls, usePlaywright);

            for (TechTrend trend : items) {
                trend.setCanonicalName(normalizationService.normalize(trend.getRawName()));
                trend.setTags(normalizationService.normalizeTags(trend.getTags()));
                repository.saveTrend(trend);
            }

            if (items.isEmpty()) {
                job.fail("Source returned no items after all fetch attempts.");
                log.warn("Job {} returned no items for source {}.", job.getJobId(), job.getSource());
            } else {
                job.complete(items.size());
                log.info("Job {} done — {} items saved.", job.getJobId(), items.size());
            }

        } catch (Exception e) {
            log.error("Job {} failed: {}", job.getJobId(), e.getMessage(), e);
            job.fail(e.getMessage());
        }

        return CompletableFuture.completedFuture(job);
    }

    public void cancelCrawl(String jobId) {
        engine.stop(jobId);
        log.info("Cancel signal sent to job {}", jobId);
    }
}
