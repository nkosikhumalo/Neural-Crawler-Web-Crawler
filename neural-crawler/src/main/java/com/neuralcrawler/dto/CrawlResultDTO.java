package com.neuralcrawler.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendDelta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/*
  FILE: CrawlResultDTO.java  (serves as TechTrendDTO)
  ======================================================
  Outgoing JSON response shape for trend data and snapshot results sent to the
  Angular frontend. Built by CrawlController from service layer data.

  CONNECTS TO:
  - CrawlController returns this from GET /api/trends and GET /api/snapshot/latest.
  - TrendAnalysisService provides topRising and topDeclining.
  - CrawlResultRepository provides the trends list.
  - Angular HttpClient parses this JSON structure.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrawlResultDTO {

    private String snapshotId;
    private String triggeredBy;
    private LocalDateTime startedAt;
    private LocalDateTime snapshotAt;
    private List<String> sourcesRan;
    private String status;
    private int totalItems;
    private String errorNotes;

    /** Full list of TechTrend records for this snapshot. */
    private List<TechTrend> trends;

    /** Top N technologies with the highest positive growth velocity. */
    private List<TrendDelta> topRising;

    /** Top N technologies with the largest decline. */
    private List<TrendDelta> topDeclining;

    private LocalDateTime lastUpdated;

    /** Human-readable message — used for error or info states. */
    private String message;
}
