package com.neuralcrawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
  FILE: TrendDelta.java
  =======================
  Domain model representing the calculated growth or decline of a technology
  between two consecutive TechSnapshot runs.

  CONNECTS TO:
  - TrendAnalysisService creates TrendDelta objects by comparing two TechSnapshot runs.
  - CrawlController exposes TrendDelta lists via GET /api/trends/deltas.
  - ExportService includes delta columns in CSV/JSON exports.
  - TechCategory and Momentum enums are companion types in this package.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class TrendDelta {

    private String canonicalName;
    private TechCategory category;
    private String previousSnapshotId;
    private String currentSnapshotId;
    private Long previousStars;
    private Long currentStars;
    private Long starDelta;
    private Double growthPercent;
    private Integer mentionDelta;
    private Momentum momentum;
    private LocalDateTime calculatedAt;

    public static TrendDelta compute(
            String canonicalName,
            TechCategory category,
            String previousSnapshotId,
            String currentSnapshotId,
            Long previousStars,
            Long currentStars,
            Integer previousMentions,
            Integer currentMentions) {

        long starDiff = (currentStars != null ? currentStars : 0L)
                - (previousStars != null ? previousStars : 0L);

        int mentionDiff = (currentMentions != null ? currentMentions : 0)
                - (previousMentions != null ? previousMentions : 0);

        double pct = 0.0;
        if (previousStars != null && previousStars > 0) {
            pct = ((double) starDiff) / previousStars * 100.0;
        }

        Momentum currentMomentum;
        if (starDiff > 0) {
            currentMomentum = Momentum.RISING;
        } else if (starDiff < 0) {
            currentMomentum = Momentum.DECLINING;
        } else {
            currentMomentum = Momentum.STABLE;
        }

        return TrendDelta.builder()
                .canonicalName(canonicalName)
                .category(category)
                .previousSnapshotId(previousSnapshotId)
                .currentSnapshotId(currentSnapshotId)
                .previousStars(previousStars)
                .currentStars(currentStars)
                .starDelta(starDiff)
                .growthPercent(pct)
                .mentionDelta(mentionDiff)
                .momentum(currentMomentum)
                .calculatedAt(LocalDateTime.now())
                .build();
    }
}
