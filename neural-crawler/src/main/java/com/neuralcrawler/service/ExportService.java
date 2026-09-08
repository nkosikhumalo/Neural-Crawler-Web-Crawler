package com.neuralcrawler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neuralcrawler.dao.CrawlResultRepository;
import com.neuralcrawler.export.ExportRequest;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendDelta;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
  FILE: ExportService.java
  ==========================
  Fetches data from the repository and delegates to format-specific exporters.
  Returns byte[] + MIME type for the controller to serve as a file download.

  CONNECTS TO:
  - CrawlController calls export() for /api/export/csv and /api/export/json.
  - CrawlResultRepository provides TechTrend and TrendDelta data.
  - ExportRequest carries the snapshot target, format, and filter options.
*/
@Service
public class ExportService {

    private final CrawlResultRepository repository;
    private final ObjectMapper objectMapper;

    public ExportService(CrawlResultRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public record ExportResult(byte[] data, String mimeType, String filename) {}

    /**
     * Resolves the requested snapshot, applies filters, and exports in the
     * requested format. Returns an ExportResult the controller streams to the client.
     */
    public ExportResult export(ExportRequest request) {
        String snapshotId = resolveSnapshotId(request);
        List<TechTrend> trends = repository.findTrendsBySnapshot(snapshotId);
        List<TrendDelta> deltas = request.isIncludeDeltas()
                ? repository.findDeltasBySnapshot(snapshotId)
                : List.of();

        // Apply source filter
        if (request.getSources() != null && !request.getSources().isEmpty()) {
            trends = trends.stream()
                    .filter(t -> request.getSources().contains(t.getSource().name()))
                    .toList();
        }

        // Apply category filter
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            trends = trends.stream()
                    .filter(t -> t.getCategory() != null
                              && request.getCategories().contains(t.getCategory().name()))
                    .toList();
        }

        return switch (request.getFormat()) {
            case CSV  -> exportCsv(trends, snapshotId);
            case JSON -> exportJson(trends, deltas, snapshotId);
        };
    }

    private String resolveSnapshotId(ExportRequest request) {
        if (request.getSnapshotId() != null) return request.getSnapshotId();
        return repository.findLatestCompleted()
                .map(s -> s.getSnapshotId())
                .orElse("unknown");
    }

    private ExportResult exportCsv(List<TechTrend> trends, String snapshotId) {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            // Header
            writer.writeNext(new String[]{
                "canonicalName", "rawName", "category", "source",
                "starCount", "mentionCount", "downloadCount", "snapshotAt", "sourceUrl"
            });
            for (TechTrend t : trends) {
                writer.writeNext(new String[]{
                    str(t.getCanonicalName()), str(t.getRawName()),
                    str(t.getCategory()), str(t.getSource()),
                    str(t.getStarCount()), str(t.getMentionCount()),
                    str(t.getDownloadCount()), str(t.getSnapshotAt()), str(t.getSourceUrl())
                });
            }
        } catch (IOException e) {
            return new ExportResult("error".getBytes(StandardCharsets.UTF_8),
                    "text/plain", "error.txt");
        }
        byte[] data = sw.toString().getBytes(StandardCharsets.UTF_8);
        return new ExportResult(data, "text/csv", "radar-" + snapshotId + ".csv");
    }

    private ExportResult exportJson(List<TechTrend> trends, List<TrendDelta> deltas, String snapshotId) {
        try {
            Map<String, Object> payload = Map.of(
                    "snapshotId", snapshotId,
                    "totalItems", trends.size(),
                    "trends", trends,
                    "deltas", deltas
            );
            byte[] data = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
            return new ExportResult(data, "application/json", "radar-" + snapshotId + ".json");
        } catch (Exception e) {
            return new ExportResult("{}".getBytes(StandardCharsets.UTF_8),
                    "application/json", "error.json");
        }
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }
}
