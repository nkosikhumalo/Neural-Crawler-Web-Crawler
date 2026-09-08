package com.neuralcrawler.crawler;

import com.microsoft.playwright.*;
import com.neuralcrawler.util.UrlUtils;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
  FILE: PlaywrightFetcherService.java
  =====================================
  Headless Chromium fetcher for JavaScript-rendered pages. Drop-in alternative
  to HttpFetcherService — returns the same String HTML but after JS execution.

  CONNECTS TO:
  - CrawlerEngine selects this when radar.sources.*.use-playwright=true in properties.
  - Source parsers receive identical String HTML — no changes needed in parsers.
  - @PreDestroy closes the browser and Playwright instance on app shutdown.
*/
@Service
public class PlaywrightFetcherService {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightFetcherService.class);

    @Value("${playwright.timeout-ms:15000}")
    private int timeoutMs;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    /**
     * Lazily initialises the Playwright browser on first call.
     * Reuses the same BrowserContext for all subsequent requests.
     */
    private synchronized BrowserContext getContext() {
        if (playwright == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (compatible; NeuralCrawler/1.0)")
                    .setJavaScriptEnabled(true)
            );
            log.info("Playwright browser initialised.");
        }
        return context;
    }

    /**
     * Navigates to the given URL in a headless browser, waits for the page to
     * settle, then returns the fully-rendered HTML as a String.
     * Returns null if navigation fails or times out.
     */
    public String fetch(String url) {
        try {
            Page page = getContext().newPage();
            page.setDefaultTimeout(timeoutMs);
            page.navigate(url);
            // Wait until no network activity for 500ms — signals JS has finished
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
            String html = page.content();
            page.close();
            return html;
        } catch (PlaywrightException e) {
            log.error("Playwright failed to fetch {}: {}", url, e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        log.info("Playwright browser closed.");
    }
}
