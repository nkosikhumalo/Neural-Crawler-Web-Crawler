
/*
  FILE: NormalizationService.java
  =================================
  Service responsible for mapping raw, free-form tech names and tags scraped from
  source pages into consistent canonical identifiers.

  WHAT IT DOES:
  - Maintains a normalization dictionary (loaded from a JSON or YAML file at startup,
    path configured via radar.normalization.map-file in application.properties) that
    maps known aliases to canonical names:
        "Golang"  → "Go"
        "go"      → "Go"
        "GoLang"  → "Go"
        "nodejs"  → "Node.js"
        "node"    → "Node.js"
        "k8s"     → "Kubernetes"
        "ts"      → "TypeScript"
        ... and hundreds more entries
  - Exposes normalize(rawName) which:
      1. Lowercases and trims the input.
      2. Looks up in the dictionary for an exact match.
      3. Falls back to a fuzzy/substring match for close variants.
      4. Returns the canonical name if found, or a title-cased version of the
         input as a best-effort fallback (and logs it for dictionary review).
  - Exposes normalizeTags(List<String>) to bulk-process a TechTrend's raw tags list.
  - Tracks unmapped terms in a running set so new slang or aliases can be reviewed
    and added to the dictionary in future updates.

  WHY IT EXISTS:
  Without normalization, "Go", "Golang", "go", and "GoLang" produce four separate
  entries in the database with fragmented metrics that never aggregate. One canonical
  name per technology is the foundation of accurate trend comparison over time.

  CONNECTS TO:
  - GitHubTrendingParser and HackerNewsParser call normalizeTags() on raw scraped tags.
  - TechTrend.canonicalName is set by calling normalize() on TechTrend.rawName.
  - TrendAnalysisService depends on canonical names being consistent across snapshots
    to correctly match a tech's current record against its previous snapshot record.
  - application.properties provides the path to the normalization dictionary file.
*/
