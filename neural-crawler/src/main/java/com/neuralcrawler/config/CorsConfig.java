
/*
  FILE: CorsConfig.java
  =======================
  Spring MVC configuration class that sets up Cross-Origin Resource Sharing (CORS)
  globally for all API endpoints.

  WHAT IT DOES:
  - Implements WebMvcConfigurer and overrides addCorsMappings() to apply CORS rules
    to all paths under /api/**.
  - Configures allowed origins from application.properties (radar.cors.allowed-origins),
    defaulting to http://localhost:4200 for the Angular development server.
  - Sets allowed HTTP methods: GET, POST, DELETE, OPTIONS.
  - Sets allowed headers: Content-Type, Authorization, X-Requested-With.
  - Enables credentials: false (no cookies needed for this API).
  - Sets max age (pre-flight cache duration) to 3600 seconds.

  WHY IT EXISTS:
  Browsers enforce the Same-Origin Policy — when the Angular app running on
  localhost:4200 makes a fetch() call to the Spring Boot API on localhost:8080,
  the browser blocks the response unless the server explicitly permits that origin
  via CORS headers. Without this config, every API call from Angular fails silently
  in the browser with a CORS error, even though the backend processed the request fine.

  CONNECTS TO:
  - All endpoints in CrawlController are covered by this CORS config via the /api/** mapping.
  - application.properties supplies the allowed origin URL(s) so it can differ between
    dev (localhost:4200) and production (the deployed Angular domain).
  - Angular's HttpClient relies on the CORS headers set here to receive API responses.
*/
