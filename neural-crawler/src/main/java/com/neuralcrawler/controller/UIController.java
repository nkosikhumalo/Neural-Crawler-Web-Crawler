
/*
  FILE: UIController.java
  =========================
  Spring MVC controller responsible for serving the Thymeleaf HTML views
  (the actual web pages the user sees in a browser).

  WHAT IT DOES:
  - Maps GET / to the main dashboard page (index.html Thymeleaf template).
  - Maps GET /results to a results page if server-side rendering is preferred over
    fetching results via the REST API in the frontend.
  - Can inject model attributes (e.g., page title, current crawl status summary)
    into templates before rendering them.

  WHY IT EXISTS:
  Separates page-serving concerns from API/data concerns. CrawlController handles
  REST calls from JavaScript; UIController handles browser navigation and full-page
  HTML renders. Having two controllers keeps each focused and clean.

  CONNECTS TO:
  - Thymeleaf templates in src/main/resources/templates/ — specifically index.html
    and results.html.
  - CrawlerService for any model data that needs to be injected into server-rendered views.
  - Spring DispatcherServlet routes browser requests here automatically based on URL mappings.
*/
