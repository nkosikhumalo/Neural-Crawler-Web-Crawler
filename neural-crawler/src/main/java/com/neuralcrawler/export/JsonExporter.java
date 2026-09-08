package com.neuralcrawler.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendDelta;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
  FILE: JsonExporter.java
  =========================
  Handles only JSON serialization of TechTrend and TrendDelta data.
  Called by ExportService — no data fetching.
*/
@Component
public class JsonExporter {

    private final ObjectMapper mapper;

    public JsonExporter() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public byte[] toJson(String snapshotId, List<TechTrend> trends, List<TrendDelta> deltas) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("snapshotId", snapshotId);
            payload.put("totalItems", trends.size());
            payload.put("trends", trends);
            if (deltas != null && !deltas.isEmpty()) {
                payload.put("deltas", deltas);
            }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
        } catch (Exception e) {
            return "{}".getBytes();
        }
    }
}
