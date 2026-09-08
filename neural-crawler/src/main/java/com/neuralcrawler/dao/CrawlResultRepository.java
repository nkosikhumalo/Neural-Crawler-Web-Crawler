package com.neuralcrawler.dao;

import com.neuralcrawler.model.SnapshotStatus;
import com.neuralcrawler.model.TechSnapshot;
import com.neuralcrawler.model.TechTrend;
import com.neuralcrawler.model.TrendDelta;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/*
  FILE: CrawlResultRepository.java
  ===================================
  In-memory repository for TechTrend, TechSnapshot, and TrendDelta records.
  Thread-safe using ConcurrentHashMap. No database required for dev/test.
  Replace with Spring Data JPA interfaces when persistence is needed.

  CONNECTS TO:
  - CrawlerService saves TechTrend records here.
  - SchedulerService saves/updates TechSnapshot records here.
  - TrendAnalysisService reads the two latest snapshots and saves TrendDelta records.
  - CrawlController reads data here via service layer for API responses.
  - ExportService reads TechTrend records here for CSV/JSON export.
*/
@Repository
public class CrawlResultRepository {

    // snapshotId → TechSnapshot
    private final ConcurrentHashMap<String, TechSnapshot> snapshots = new ConcurrentHashMap<>();

    // snapshotId → list of TechTrend records for that snapshot
    private final ConcurrentHashMap<String, List<TechTrend>> trends = new ConcurrentHashMap<>();

    // snapshotId → list of TrendDelta records computed for that snapshot
    private final ConcurrentHashMap<String, List<TrendDelta>> deltas = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // TechSnapshot operations
    // -------------------------------------------------------------------------

    public void saveSnapshot(TechSnapshot snapshot) {
        snapshots.put(snapshot.getSnapshotId(), snapshot);
    }

    public Optional<TechSnapshot> findSnapshot(String snapshotId) {
        return Optional.ofNullable(snapshots.get(snapshotId));
    }

    /** Returns the most recently completed snapshot, or empty if none exists. */
    public Optional<TechSnapshot> findLatestCompleted() {
        return snapshots.values().stream()
                .filter(s -> s.getStatus() == SnapshotStatus.COMPLETED
                          || s.getStatus() == SnapshotStatus.PARTIAL)
                .max(Comparator.comparing(TechSnapshot::getCompletedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** Returns the two most recently completed snapshots for delta computation. */
    public List<TechSnapshot> findTwoLatestCompleted() {
        return snapshots.values().stream()
                .filter(s -> s.getStatus() == SnapshotStatus.COMPLETED
                          || s.getStatus() == SnapshotStatus.PARTIAL)
                .sorted(Comparator.comparing(TechSnapshot::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(2)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // TechTrend operations
    // -------------------------------------------------------------------------

    public void saveTrend(TechTrend trend) {
        trends.computeIfAbsent(trend.getSnapshotId(), k -> Collections.synchronizedList(new ArrayList<>()))
              .add(trend);
    }

    public List<TechTrend> findTrendsBySnapshot(String snapshotId) {
        return new ArrayList<>(trends.getOrDefault(snapshotId, List.of()));
    }

    /** Finds a trend by canonical name and snapshot — used for delta pairing. */
    public Optional<TechTrend> findTrend(String snapshotId, String canonicalName,
                                          com.neuralcrawler.model.TrendSource source) {
        return findTrendsBySnapshot(snapshotId).stream()
                .filter(t -> canonicalName.equalsIgnoreCase(t.getCanonicalName())
                          && t.getSource() == source)
                .findFirst();
    }

    // -------------------------------------------------------------------------
    // TrendDelta operations
    // -------------------------------------------------------------------------

    public void saveDelta(TrendDelta delta) {
        deltas.computeIfAbsent(delta.getCurrentSnapshotId(),
                k -> Collections.synchronizedList(new ArrayList<>())).add(delta);
    }

    public List<TrendDelta> findDeltasBySnapshot(String snapshotId) {
        return new ArrayList<>(deltas.getOrDefault(snapshotId, List.of()));
    }

    /** Top N deltas sorted by growthPercent descending (rising techs). */
    public List<TrendDelta> findTopRising(String snapshotId, int limit) {
        return findDeltasBySnapshot(snapshotId).stream()
                .filter(d -> d.getGrowthPercent() != null && d.getGrowthPercent() > 0)
                .sorted(Comparator.comparingDouble(TrendDelta::getGrowthPercent).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** Top N deltas sorted by growthPercent ascending (declining techs). */
    public List<TrendDelta> findTopDeclining(String snapshotId, int limit) {
        return findDeltasBySnapshot(snapshotId).stream()
                .filter(d -> d.getGrowthPercent() != null && d.getGrowthPercent() < 0)
                .sorted(Comparator.comparingDouble(TrendDelta::getGrowthPercent))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** Removes all data older than the given cutoff to prevent unbounded memory growth. */
    public void deleteOlderThan(LocalDateTime cutoff) {
        snapshots.values().stream()
                .filter(s -> s.getCompletedAt() != null && s.getCompletedAt().isBefore(cutoff))
                .forEach(s -> {
                    String id = s.getSnapshotId();
                    snapshots.remove(id);
                    trends.remove(id);
                    deltas.remove(id);
                });
    }
}
