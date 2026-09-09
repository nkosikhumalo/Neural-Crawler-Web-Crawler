
/*
  FILE: app.js  — DEPRECATED / PLACEHOLDER
  ==========================================
  This file is no longer the frontend application layer.

  WHAT REPLACED IT:
  The Angular SPA is the frontend for the Tech Stack Radar. It runs as a separate
  project on its own dev server (default port 4200) and communicates with the Spring
  Boot backend exclusively through the REST API endpoints in CrawlController.

  The Angular app handles:
  - The radar dashboard displaying trending technologies
  - Rising/declining tech leaderboards sourced from GET /api/trends/rising
  - Snapshot history timeline sourced from GET /api/snapshot/{id}
  - Manual crawl trigger button (POST /api/crawl/trigger)
  - CSV/JSON export download buttons

  WHY THIS FILE STILL EXISTS:
  Spring Boot's static resource serving is still configured, so this file can hold
  a minimal redirect or placeholder page at the root that points users to the Angular
  app URL if they accidentally hit the backend port directly in a browser.

  CONNECTS TO:
  - CorsConfig enables the Angular origin to call the Spring Boot API cross-origin.
  - CrawlController exposes all the REST endpoints the Angular app consumes.
  - The Angular project lives in a separate repository / workspace folder.
*/
