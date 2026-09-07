
/*
  FILE: ExportService.java
  ==========================
  Orchestration service for export operations — the single entry point the controller
  calls, which then delegates to the appropriate format-specific exporter.

  WHAT IT DOES:
  - Accepts an ExportRequest from CrawlController.
  - Queries CrawlResultRepository to fetch the TechTrend list for the requested
    snapshotId (or the latest snapshot if snapshotId is null).
  - Applies any source or category filters specified in the ExportRequest.
  - Optionally fetches TrendDelta records from the repository if includeDeltas is true.
  - Delegates to CsvExporter or JsonExporter based on ExportRequest.format.
  - Returns the resulting byte[] with the correct MIME type string so the controller
    can set the Content-Type and Content-Disposition headers on the HTTP response.
  - Handles the edge case of an empty result set — returns a valid but empty
    CSV header row or empty JSON array rather than throwing.

  WHY IT EXISTS:
  Keeps the controller free of any serialization logic and keeps each exporter free
  of any data-fetching logic. This service is the glue between data access and format
  serialization.

  CONNECTS TO:
  - CrawlController calls export(ExportRequest) here for both /api/export/csv and json.
  - CrawlResultRepository provides the TechTrend and TrendDelta data.
  - CsvExporter handles CSV byte[] production.
  - JsonExporter handles JSON byte[] production.
  - ExportRequest carries the filter and format parameters.
*/
