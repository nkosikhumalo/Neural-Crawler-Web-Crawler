package com.neuralcrawler.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/*
  FILE: NormalizationService.java
  =================================
  Maps raw scraped tech names into consistent canonical identifiers using the
  tech-aliases.json dictionary. Called on every TechTrend before it is saved.

  CONNECTS TO:
  - CrawlerService calls normalize() on each TechTrend after extraction.
  - tech-aliases.json in src/main/resources provides the alias → canonical map.
  - TechTrend.canonicalName is set to the result of normalize(rawName).
*/
@Service
public class NormalizationService {

    private static final Logger log = LoggerFactory.getLogger(NormalizationService.class);

    @Value("${radar.normalization.map-file:tech-aliases.json}")
    private String mapFile;

    private final ObjectMapper objectMapper;

    // lowercase alias → canonical name
    private Map<String, String> aliasMap = new HashMap<>();

    // terms with no mapping — for dictionary review logging
    private final Set<String> unmappedTerms = Collections.synchronizedSet(new HashSet<>());

    public NormalizationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadDictionary() {
        try {
            ClassPathResource resource = new ClassPathResource(mapFile);
            try (InputStream is = resource.getInputStream()) {
                Map<String, String> raw = objectMapper.readValue(is, new TypeReference<>() {});
                aliasMap = new HashMap<>();
                raw.forEach((alias, canonical) -> aliasMap.put(alias.toLowerCase(), canonical));
                log.info("Normalization dictionary loaded: {} entries from {}", aliasMap.size(), mapFile);
            }
        } catch (IOException e) {
            log.warn("Could not load normalization map '{}' — normalization will use raw names. Error: {}",
                    mapFile, e.getMessage());
        }
    }

    /**
     * Maps a raw tech name to its canonical identifier.
     * 1. Exact lowercase match in the dictionary.
     * 2. Title-cased fallback if no match found (logs the unmapped term).
     */
    public String normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) return rawName;

        String key = rawName.trim().toLowerCase();
        String canonical = aliasMap.get(key);

        if (canonical != null) return canonical;

        // Log unmapped terms for dictionary maintenance
        if (unmappedTerms.add(key)) {
            log.debug("Unmapped tech name: '{}' — using as-is", rawName);
        }

        // Best-effort: title-case the raw name
        return toTitleCase(rawName.trim());
    }

    /**
     * Normalizes a list of raw tags, returning a deduplicated list of canonical names.
     */
    public List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null) return List.of();
        return rawTags.stream()
                .map(this::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** Returns a read-only snapshot of all unmapped terms encountered so far. */
    public Set<String> getUnmappedTerms() {
        return Collections.unmodifiableSet(unmappedTerms);
    }

    private String toTitleCase(String input) {
        if (input.isBlank()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
