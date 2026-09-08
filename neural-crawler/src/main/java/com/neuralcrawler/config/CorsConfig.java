package com.neuralcrawler.config;

/*
  FILE: CorsConfig.java
  =======================
  CORS is now handled entirely inside SecurityConfig.corsConfigurationSource()
  so Spring Security processes it before any auth checks.

  This class is intentionally empty — kept as a placeholder so the file path
  remains valid in case it is referenced elsewhere.
*/
public class CorsConfig {
    // CORS configuration moved to SecurityConfig to avoid conflict with
    // Spring Security's filter chain. See SecurityConfig.corsConfigurationSource()
}
