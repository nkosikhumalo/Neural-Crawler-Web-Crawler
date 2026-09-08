package com.neuralcrawler;

import com.neuralcrawler.crawler.CrawlerEngine;
import com.neuralcrawler.dao.CrawlResultRepository;
import com.neuralcrawler.model.*;
import com.neuralcrawler.service.CrawlerService;
import com.neuralcrawler.service.NormalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerServiceTest {

    @Mock
    private CrawlerEngine engine;

    @Mock
    private NormalizationService normalizationService;

    @Mock
    private CrawlResultRepository repository;

    @InjectMocks
    private CrawlerService crawlerService;

    private CrawlJob job;
    private List<String> seedUrls;

    @BeforeEach
    void setUp() {
        job = CrawlJob.create("snapshot-123", TrendSource.GITHUB);
        seedUrls = List.of("https://github.com/trending");
    }

    @Test
    void runSourceCrawl_transitionsJobToCompleted_whenEngineSucceeds() throws Exception {
        TechTrend trend = TechTrend.of("rust", TrendSource.GITHUB, "snapshot-123");
        when(engine.crawl(any(), any(), anyBoolean())).thenReturn(List.of(trend));
        when(normalizationService.normalize(anyString())).thenReturn("Rust");
        when(normalizationService.normalizeTags(any())).thenReturn(List.of("Rust"));

        CompletableFuture<CrawlJob> future = crawlerService.runSourceCrawl(job, seedUrls);
        CrawlJob result = future.get();

        assertThat(result.getStatus()).isEqualTo(CrawlStatus.COMPLETED);
        assertThat(result.getItemsFound()).isEqualTo(1);
        verify(repository, times(1)).saveTrend(any(TechTrend.class));
    }

    @Test
    void runSourceCrawl_normalizesEachTrend() throws Exception {
        TechTrend t1 = TechTrend.of("golang", TrendSource.GITHUB, "snapshot-123");
        TechTrend t2 = TechTrend.of("nodejs", TrendSource.GITHUB, "snapshot-123");
        when(engine.crawl(any(), any(), anyBoolean())).thenReturn(List.of(t1, t2));
        when(normalizationService.normalize(anyString())).thenReturn("Normalized");
        when(normalizationService.normalizeTags(any())).thenReturn(List.of());

        crawlerService.runSourceCrawl(job, seedUrls).get();

        verify(normalizationService, times(2)).normalize(anyString());
    }

    @Test
    void runSourceCrawl_setsJobToFailed_whenEngineThrows() throws Exception {
        when(engine.crawl(any(), any(), anyBoolean())).thenThrow(new RuntimeException("Network error"));

        CompletableFuture<CrawlJob> future = crawlerService.runSourceCrawl(job, seedUrls);
        CrawlJob result = future.get();

        assertThat(result.getStatus()).isEqualTo(CrawlStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("Network error");
    }

    @Test
    void runSourceCrawl_noItems_marksJobFailedWithZeroCount() throws Exception {
        when(engine.crawl(any(), any(), anyBoolean())).thenReturn(List.of());

        CompletableFuture<CrawlJob> future = crawlerService.runSourceCrawl(job, seedUrls);
        CrawlJob result = future.get();

        assertThat(result.getStatus()).isEqualTo(CrawlStatus.FAILED);
        assertThat(result.getItemsFound()).isZero();
        verify(repository, never()).saveTrend(any());
    }
}
