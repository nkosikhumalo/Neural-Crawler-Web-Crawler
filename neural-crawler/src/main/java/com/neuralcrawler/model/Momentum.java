package com.neuralcrawler.model;

/*
  FILE: Momentum.java
  =====================
  Enum representing the directional trend of a technology between two consecutive
  snapshots, as computed by TrendAnalysisService.

  Values:
  - RISING    — positive growth percent above the configured threshold
  - STABLE    — change within the threshold range (neither gaining nor losing)
  - DECLINING — negative growth (losing stars or mentions)
  - NEW       — tech appears in the current snapshot but had no previous record
  - DROPPED   — tech was in the previous snapshot but absent from the current one

  CONNECTS TO:
  - TrendDelta.momentum is set to one of these values by TrendAnalysisService.
  - TrendDelta.compute() sets RISING, STABLE, or DECLINING based on starDelta.
  - TrendAnalysisService sets NEW or DROPPED for first/last appearances.
  - Angular frontend uses these values to render directional indicators and badges.
*/
public enum Momentum {
    RISING,
    STABLE,
    DECLINING,
    NEW,
    DROPPED
}
