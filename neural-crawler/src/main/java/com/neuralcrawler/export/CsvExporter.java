package com.neuralcrawler.export;

import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendDelta;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/*
  FILE: CsvExporter.java
  ========================
  Handles only CSV serialization of TechTrend and TrendDelta data.
  Called by ExportService — no data fetching, no Spring dependencies beyond @Component.
*/
@Component
public class CsvExporter {

    public byte[] toCsv(List<TechTrend> trends, List<TrendDelta> deltas) {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writeTrends(writer, trends);
            if (deltas != null && !deltas.isEmpty()) {
                writer.writeNext(new String[]{});  // blank separator row
                writeDeltas(writer, deltas);
            }
        } catch (IOException e) {
            return new byte[0];
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void writeTrends(CSVWriter writer, List<TechTrend> trends) {
        writer.writeNext(new String[]{
            "canonicalName", "rawName", "category", "source",
            "starCount", "mentionCount", "downloadCount", "snapshotAt", "sourceUrl"
        });
        for (TechTrend t : trends) {
            writer.writeNext(new String[]{
                s(t.getCanonicalName()), s(t.getRawName()),
                s(t.getCategory()), s(t.getSource()),
                s(t.getStarCount()), s(t.getMentionCount()),
                s(t.getDownloadCount()), s(t.getSnapshotAt()), s(t.getSourceUrl())
            });
        }
    }

    private void writeDeltas(CSVWriter writer, List<TrendDelta> deltas) {
        writer.writeNext(new String[]{
            "canonicalName", "category", "previousSnapshotId", "currentSnapshotId",
            "previousStars", "currentStars", "starDelta", "growthPercent",
            "mentionDelta", "momentum", "calculatedAt"
        });
        for (TrendDelta d : deltas) {
            writer.writeNext(new String[]{
                s(d.getCanonicalName()), s(d.getCategory()),
                s(d.getPreviousSnapshotId()), s(d.getCurrentSnapshotId()),
                s(d.getPreviousStars()), s(d.getCurrentStars()),
                s(d.getStarDelta()), s(d.getGrowthPercent()),
                s(d.getMentionDelta()), s(d.getMomentum()), s(d.getCalculatedAt())
            });
        }
    }

    private String s(Object o) {
        return o != null ? o.toString() : "";
    }
}
