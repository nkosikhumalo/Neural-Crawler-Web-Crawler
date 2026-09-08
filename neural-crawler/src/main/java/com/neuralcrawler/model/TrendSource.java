package com.neuralcrawler.model;

/*
  FILE: TrendSource.java
  ========================
  Enum identifying which developer platform a TechTrend record or CrawlJob
  was sourced from.

  CONNECTS TO:
  - TechTrend.source uses this enum.
  - CrawlJob.source uses this enum to identify which platform the job is crawling.
  - CrawlerEngine selects the correct parser based on this value.
  - SelectorConfig groups selector strings per source using these values.
  - ExportRequest.sources filters export output by these values.
*/
public enum TrendSource {
    GITHUB,
    HACKERNEWS,
    HACKERNEWS_API,
    MAVEN_CENTRAL,
    STACKOVERFLOW_JOBS
}
