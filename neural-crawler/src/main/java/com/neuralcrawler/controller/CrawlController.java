package com.neuralcrawler.controller;

import com.neuralcrawler.dao.CrawlResultRepository;
import com.neuralcrawler.dto.CrawlRequestDTO;
import com.neuralcrawler.dto.CrawlResultDTO;
import com.neuralcrawler.export.ExportRequest;
import com.neuralcrawler.model.TechSnapshot;
import com.neuralcrawler.service.CrawlerService;
import com.neuralcrawler.service.ExportService;
import com.neuralcrawler.service.SchedulerService;
import com.neuralcrawler.service.TrendAnalysisService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
  FILE: CrawlController.java
  ============================
  REST controller — the entire API surface consumed by the Angular frontend.
  Thin layer: validates input, delegates to services, formats HTTP responses.
*/
@RestController
@RequestMapping("/api")
public class CrawlController {

    private final SchedulerService schedulerService;
    private final CrawlerService crawlerService;
    private final TrendAnalysisService analysisService;
    private final CrawlResultRepository repository;
    private final ExportService exportService;

    public CrawlController(SchedulerService schedulerService,
                           CrawlerService crawlerService,
                           TrendAnalysisService analysisService,
                           CrawlResultRepository repository,
                           ExportService exportService) {
        this.schedulerService = schedulerService;
        this.crawlerService = crawlerService;
        this.analysisService = analysisService;
        this.repository = repository;
        this.exportService = exportService;
    }

    // -------------------------------------------------------------------------
    // Trigger & status
    // -------------------------------------------------------------------------

    /** Manually trigger a full radar run. Returns 202 immediately with JSON — crawl runs in background. */
    @PostMapping("/crawl/trigger")
    public ResponseEntity<java.util.Map<String, Object>> trigger(
            @RequestBody(required = false) CrawlRequestDTO request) {
        List<String> sources = request != null ? request.getSources() : null;

        if (!schedulerService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("message", "A crawl is already in progress.", "status", "BUSY"));
        }

        schedulerService.manualTrigger(sources);

        return ResponseEntity.accepted()
                .body(java.util.Map.of(
                    "message", "Radar run started successfully.",
                    "status", "ACCEPTED"
                ));
    }

    /** Cancel an active crawl job by jobId. */
    @DeleteMapping("/crawl/{jobId}")
    public ResponseEntity<Void> cancel(@PathVariable String jobId) {
        crawlerService.cancelCrawl(jobId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Snapshot endpoints
    // -------------------------------------------------------------------------

    /** Returns the latest completed TechSnapshot with trend and delta data. */
    @GetMapping("/snapshot/latest")
    public ResponseEntity<CrawlResultDTO> latestSnapshot(
            @RequestParam(defaultValue = "10") int topN) {
        return repository.findLatestCompleted()
                .map(snapshot -> buildResponse(snapshot, topN))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Returns a specific snapshot by ID. */
    @GetMapping("/snapshot/{snapshotId}")
    public ResponseEntity<CrawlResultDTO> getSnapshot(
            @PathVariable String snapshotId,
            @RequestParam(defaultValue = "10") int topN) {
        return repository.findSnapshot(snapshotId)
                .map(snapshot -> buildResponse(snapshot, topN))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------------------------
    // Trend data endpoints
    // -------------------------------------------------------------------------

    /** Returns the full TechTrend list for the latest snapshot. */
    @GetMapping("/trends")
    public ResponseEntity<CrawlResultDTO> trends() {
        return repository.findLatestCompleted()
                .map(s -> CrawlResultDTO.builder()
                        .snapshotId(s.getSnapshotId())
                        .triggeredBy(s.getTriggeredBy())
                        .startedAt(s.getStartedAt())
                        .snapshotAt(s.getCompletedAt())
                        .sourcesRan(s.getSourcesRan())
                        .status(s.getStatus().name())
                        .totalItems(s.getTotalItems())
                        .errorNotes(s.getErrorNotes())
                        .trends(repository.findTrendsBySnapshot(s.getSnapshotId()))
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Returns top N rising technologies from the latest snapshot. */
    @GetMapping("/trends/rising")
    public ResponseEntity<?> rising(@RequestParam(defaultValue = "10") int limit) {
        return repository.findLatestCompleted()
                .map(s -> analysisService.getTopRising(s.getSnapshotId(), limit))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Returns top N declining technologies from the latest snapshot. */
    @GetMapping("/trends/declining")
    public ResponseEntity<?> declining(@RequestParam(defaultValue = "10") int limit) {
        return repository.findLatestCompleted()
                .map(s -> analysisService.getTopDeclining(s.getSnapshotId(), limit))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------------------------
    // Export endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String snapshotId,
            @RequestParam(defaultValue = "false") boolean includeDeltas) {
        ExportService.ExportResult result = exportService.export(
                ExportRequest.builder()
                        .snapshotId(snapshotId)
                        .format(ExportRequest.ExportFormat.CSV)
                        .includeDeltas(includeDeltas)
                        .build());
        return download(result);
    }

    @GetMapping("/export/json")
    public ResponseEntity<byte[]> exportJson(
            @RequestParam(required = false) String snapshotId,
            @RequestParam(defaultValue = "true") boolean includeDeltas) {
        ExportService.ExportResult result = exportService.export(
                ExportRequest.builder()
                        .snapshotId(snapshotId)
                        .format(ExportRequest.ExportFormat.JSON)
                        .includeDeltas(includeDeltas)
                        .build());
        return download(result);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CrawlResultDTO buildResponse(TechSnapshot snapshot, int topN) {
        String id = snapshot.getSnapshotId();
        return CrawlResultDTO.builder()
                .snapshotId(id)
                .triggeredBy(snapshot.getTriggeredBy())
                .startedAt(snapshot.getStartedAt())
                .snapshotAt(snapshot.getCompletedAt())
                .sourcesRan(snapshot.getSourcesRan())
                .status(snapshot.getStatus().name())
                .totalItems(snapshot.getTotalItems())
                .errorNotes(snapshot.getErrorNotes())
                .trends(repository.findTrendsBySnapshot(id))
                .topRising(analysisService.getTopRising(id, topN))
                .topDeclining(analysisService.getTopDeclining(id, topN))
                .build();
    }

    private ResponseEntity<byte[]> download(ExportService.ExportResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .body(result.data());
    }
}
