package com.neuralcrawler.service;

import com.neuralcrawler.dao.CrawlResultRepository;
import com.neuralcrawler.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*
  FILE: TrendAnalysisService.java
  =================================
  Computes TrendDelta records by comparing TechTrend data across the two most
  recent completed snapshots. This is what turns raw numbers into trend signals.

  CONNECTS TO:
  - SchedulerService calls computeDeltas(currentSnapshotId) after each run.
  - CrawlResultRepository provides TechTrend records and stores TrendDelta results.
  - CrawlController calls getTopRising() / getTopDeclining() for API responses.
*/
@Service
public class TrendAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(TrendAnalysisService.class);

    @Value("${radar.analysis.momentum-threshold:5.0}")
    private double momentumThreshold;

    private final CrawlResultRepository repository;

    public TrendAnalysisService(CrawlResultRepository repository) {
        this.repository = repository;
    }

    /**
     * Computes TrendDelta records for the given snapshot by comparing it against
     * the previous completed snapshot. Persists all deltas to the repository.
     */
    public void computeDeltas(String currentSnapshotId) {
        List<TechSnapshot> twoLatest = repository.findTwoLatestCompleted();
        if (twoLatest.size() < 2) {
            log.info("Not enough snapshots for delta computation (need 2, have {}).", twoLatest.size());
            return;
        }

        // Ensure current is first, previous is second
        TechSnapshot current = twoLatest.get(0);
        TechSnapshot previous = twoLatest.get(1);

        if (!current.getSnapshotId().equals(currentSnapshotId)) {
            log.warn("Latest snapshot {} differs from expected {}. Skipping delta.",
                    current.getSnapshotId(), currentSnapshotId);
            return;
        }

        List<TechTrend> currentTrends = repository.findTrendsBySnapshot(current.getSnapshotId());
        List<TechTrend> previousTrends = repository.findTrendsBySnapshot(previous.getSnapshotId());

        // Build lookup: canonicalName+source → TechTrend for previous snapshot
        Map<String, TechTrend> previousMap = previousTrends.stream()
                .collect(Collectors.toMap(
                        t -> t.getCanonicalName() + ":" + t.getSource(),
                        t -> t,
                        (a, b) -> a // keep first on duplicate
                ));

        int deltaCount = 0;

        for (TechTrend curr : currentTrends) {
            String key = curr.getCanonicalName() + ":" + curr.getSource();
            TechTrend prev = previousMap.get(key);

            TrendDelta delta;
            if (prev == null) {
                // First appearance
                delta = TrendDelta.compute(
                        curr.getCanonicalName(), curr.getCategory(),
                        null, current.getSnapshotId(),
                        null, curr.getStarCount(),
                        null, curr.getMentionCount()
                );
                delta.setMomentum(Momentum.NEW);
            } else {
                delta = TrendDelta.compute(
                        curr.getCanonicalName(), curr.getCategory(),
                        previous.getSnapshotId(), current.getSnapshotId(),
                        prev.getStarCount(), curr.getStarCount(),
                        prev.getMentionCount(), curr.getMentionCount()
                );
                // Override momentum if growthPercent is within stable threshold
                if (delta.getGrowthPercent() != null
                        && Math.abs(delta.getGrowthPercent()) < momentumThreshold) {
                    delta.setMomentum(Momentum.STABLE);
                }
            }

            repository.saveDelta(delta);
            deltaCount++;
        }

        log.info("Delta computation complete for snapshot {}. {} deltas produced.", currentSnapshotId, deltaCount);
    }

    public List<TrendDelta> getTopRising(String snapshotId, int limit) {
        return repository.findTopRising(snapshotId, limit);
    }

    public List<TrendDelta> getTopDeclining(String snapshotId, int limit) {
        return repository.findTopDeclining(snapshotId, limit);
    }
}
