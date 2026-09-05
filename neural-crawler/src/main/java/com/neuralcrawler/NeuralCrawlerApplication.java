
/*
  FILE: NeuralCrawlerApplication.java
  =====================================
  This is the application entry point — the main class that bootstraps the entire
  Spring Boot application.

  WHAT IT DOES:
  - Contains the standard `main` method that JVM calls to start the process.
  - Annotated with @SpringBootApplication which triggers component scanning, auto-configuration,
    and configuration property binding across the whole project.
  - Boots up the embedded Tomcat server so the web UI and REST endpoints become accessible.

  WHY IT EXISTS:
  Every Spring Boot application needs exactly one entry point class. This is it.
  Without it nothing starts — no server, no endpoints, no crawling.

  CONNECTS TO:
  - Spring Boot auto-configuration picks up CrawlerConfig, AsyncConfig, and all
    @RestController / @Service / @Component beans automatically.
  - Indirectly connects to every layer: controllers, services, crawler engine, parsers,
    and exporters — they all get wired together through Spring's dependency injection
    once this class fires up the context.
*/
