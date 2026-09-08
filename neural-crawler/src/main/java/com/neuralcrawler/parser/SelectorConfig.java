package com.neuralcrawler.parser;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/*
  FILE: SelectorConfig.java
  ===========================
  @ConfigurationProperties POJO binding radar.selectors.* from application.properties.
  Injected into all three source parsers. Updating a broken selector is a config
  change only — no recompile needed.

  CONNECTS TO:
  - GitHubTrendingParser injects this for GitHub-specific selectors.
  - HackerNewsParser injects this for HN selectors and the tech keyword list.
  - application.properties defines the actual values under radar.selectors.*
*/
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "radar.selectors")
public class SelectorConfig {

    // --- GitHub Trending selectors ---
    private String githubCard = "article.Box-row";
    private String githubRepoName = "h2.h3 a";
    private String githubDescription = "p.col-9";
    private String githubLanguage = "span[itemprop=programmingLanguage]";
    private String githubStarCount = "a.Link--muted:has(svg.octicon-star)";
    private String githubStarsToday = "span.d-inline-block.float-sm-right";
    private String githubTopicTag = "a.topic-tag";

    // --- Hacker News selectors ---
    private String hnStoryRow = "tr.athing";
    private String hnStoryTitle = "span.titleline a";
    private String hnScore = "span.score";
    private String hnCommentLink = "a[href*=item]";
    private String hnNextPage = "a.morelink";

    // --- Tech keyword list for HN title scanning ---
    private List<String> techKeywords = new ArrayList<>(List.of(
            "Rust", "Go", "Golang", "TypeScript", "JavaScript", "Python",
            "Kotlin", "Swift", "Zig", "Bun", "Deno", "Node.js", "Node",
            "React", "Vue", "Angular", "Svelte", "htmx", "WASM", "WebAssembly",
            "Docker", "Kubernetes", "K8s", "Terraform", "Pulumi",
            "LLM", "GPT", "Claude", "Ollama", "Llama",
            "Spring Boot", "Quarkus", "Micronaut",
            "PostgreSQL", "SQLite", "Redis", "Kafka", "Grafana",
            "Nix", "Linux", "RISC-V"
    ));
}
