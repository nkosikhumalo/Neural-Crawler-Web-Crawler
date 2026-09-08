package com.neuralcrawler.crawler;

import com.neuralcrawler.util.RateLimiter;
import com.neuralcrawler.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RobotsService {

    private static final Logger log = LoggerFactory.getLogger(RobotsService.class);

    @Value("${crawler.user-agent:NeuralCrawler/1.0 (Tech Stack Radar)}")
    private String userAgent;

    private final HttpFetcherService fetcher;
    private final RateLimiter rateLimiter;

    private final ConcurrentHashMap<String, List<String>> disallowCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> allowCache    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean>      fetchedDomains = new ConcurrentHashMap<>();

    public RobotsService(HttpFetcherService fetcher, RateLimiter rateLimiter) {
        this.fetcher = fetcher;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Returns true if the given URL is permitted by robots.txt.
     * Allow rules take precedence over Disallow when they are more specific
     * (longer path prefix) — this is the standard robots.txt resolution order
     * and is what allows Firebase's "Allow: /*.json$" to override "Disallow: /".
     */
    public boolean isAllowed(String url) {
        String domain = UrlUtils.extractDomain(url);
        if (domain == null) return false;

        ensureLoaded(domain);

        String path = extractPath(url);
        List<String> disallowed = disallowCache.getOrDefault(domain, List.of());
        List<String> allowed    = allowCache.getOrDefault(domain, List.of());

        // Find the most specific matching disallow rule
        String matchedDisallow = disallowed.stream()
                .filter(rule -> matchesRule(path, rule))
                .reduce("", (a, b) -> a.length() >= b.length() ? a : b);

        // Find the most specific matching allow rule
        String matchedAllow = allowed.stream()
                .filter(rule -> matchesRule(path, rule))
                .reduce("", (a, b) -> a.length() >= b.length() ? a : b);

        // Allow wins if it is at least as specific as the disallow rule
        if (!matchedDisallow.isEmpty() && matchedAllow.length() >= matchedDisallow.length()) {
            return true;
        }

        if (!matchedDisallow.isEmpty()) {
            log.debug("Blocked by robots.txt: {} (rule: {})", url, matchedDisallow);
            return false;
        }

        return true;
    }

    private boolean matchesRule(String path, String rule) {
        if (rule.isBlank()) return false;
        // Handle wildcard suffix pattern like /*.json$
        if (rule.contains("*")) {
            String[] parts = rule.split("\\*", -1);
            int idx = 0;
            for (String part : parts) {
                if (part.isEmpty()) continue;
                int found = path.indexOf(part, idx);
                if (found == -1) return false;
                idx = found + part.length();
            }
            return true;
        }
        // Standard prefix match
        return path.startsWith(rule);
    }

    private void ensureLoaded(String domain) {
        if (fetchedDomains.containsKey(domain)) return;
        fetchedDomains.put(domain, true); // mark before fetch to avoid race

        String robotsUrl = "https://" + domain + "/robots.txt";
        String content = fetcher.fetch(robotsUrl);

        if (content == null) {
            disallowCache.put(domain, List.of());
            allowCache.put(domain, List.of());
            return;
        }

        parseRobots(domain, content);
    }

    private void parseRobots(String domain, String content) {
        List<String> disallowed = new ArrayList<>();
        List<String> allowed    = new ArrayList<>();
        boolean applicable = false;
        String agentLower = userAgent.toLowerCase();

        for (String line : content.split("\\n")) {
            line = line.trim();
            if (line.startsWith("#") || line.isBlank()) continue;

            if (line.toLowerCase().startsWith("user-agent:")) {
                String agent = line.substring("user-agent:".length()).trim().toLowerCase();
                applicable = agent.equals("*") || agentLower.contains(agent);
                continue;
            }

            if (!applicable) continue;

            if (line.toLowerCase().startsWith("allow:")) {
                String p = line.substring("allow:".length()).trim();
                if (!p.isBlank()) allowed.add(p);
            } else if (line.toLowerCase().startsWith("disallow:")) {
                String p = line.substring("disallow:".length()).trim();
                if (!p.isBlank()) disallowed.add(p);
            } else if (line.toLowerCase().startsWith("crawl-delay:")) {
                try {
                    long delay = Long.parseLong(line.substring("crawl-delay:".length()).trim());
                    rateLimiter.setDomainDelay(domain, delay);
                } catch (NumberFormatException ignored) {}
            }
        }

        disallowCache.put(domain, disallowed);
        allowCache.put(domain, allowed);
        log.debug("robots.txt for {}: {} disallow, {} allow rules", domain, disallowed.size(), allowed.size());
    }

    private String extractPath(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String path = uri.getPath();
            return path != null ? path : "/";
        } catch (Exception e) {
            return "/";
        }
    }
}
