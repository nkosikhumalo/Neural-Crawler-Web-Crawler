package com.neuralcrawler.model;

/*
  FILE: TechCategory.java
  =========================
  Enum classifying a technology into a broad category for grouping and filtering
  in the radar dashboard and export outputs.

  CONNECTS TO:
  - TechTrend.category uses this enum.
  - TrendDelta.category uses this enum.
  - ExportRequest.categories filters by these values.
  - Angular frontend uses these values to group radar entries by type.
*/
public enum TechCategory {
    LANGUAGE,
    FRAMEWORK,
    LIBRARY,
    TOOL,
    PLATFORM,
    DATABASE,
    DEVOPS,
    AI_ML,
    OTHER
}
