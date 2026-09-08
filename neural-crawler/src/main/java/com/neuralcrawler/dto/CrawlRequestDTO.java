package com.neuralcrawler.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/*
  FILE: CrawlRequestDTO.java
  ============================
  Payload for POST /api/crawl/trigger — manual radar run requests from the Angular UI.
  Scheduled runs need no payload — SchedulerService handles those automatically.

  CONNECTS TO:
  - CrawlController deserializes this from the request body.
  - SchedulerService.manualTrigger() receives the source list from this DTO.
*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrawlRequestDTO {

    /**
     * Which sources to crawl. Valid values: "GITHUB", "HACKERNEWS", "MAVEN_CENTRAL".
     * If null or empty, all configured sources are included.
     */
    private List<String> sources;

    /**
     * Optional label for this manual run — stored in TechSnapshot.triggeredBy notes.
     */
    private String note;
}
