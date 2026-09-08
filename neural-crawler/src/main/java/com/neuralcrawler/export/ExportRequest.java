package com.neuralcrawler.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/*
  FILE: ExportRequest.java
  ==========================
  Parameters for an export operation. Built by CrawlController from query params
  and passed to ExportService to drive data fetching and format selection.

  CONNECTS TO:
  - CrawlController constructs this from request query parameters.
  - ExportService reads format, snapshotId, filters, and includeDeltas from this.
  - ExportFormat enum is defined as a nested type here.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    /** Which snapshot to export. Null = latest completed snapshot. */
    private String snapshotId;

    /** Output format. Defaults to JSON if not specified. */
    @Builder.Default
    private ExportFormat format = ExportFormat.JSON;

    /** Whether to include TrendDelta records alongside TechTrend data. */
    @Builder.Default
    private boolean includeDeltas = false;

    /** Filter by source platform. Empty list = include all sources. */
    private List<String> sources;

    /** Filter by tech category. Empty list = include all categories. */
    private List<String> categories;

    public enum ExportFormat {
        CSV, JSON
    }
}
