
/*
  FILE: SelectorConfig.java
  ===========================
  A configuration/data class that holds the CSS selector strings and XPath expressions
  used by HtmlParserService to locate data on target web pages.

  WHAT IT DOES:
  - Defines fields for each piece of data to extract: titleSelector, priceSelector,
    availabilitySelector, nextPageSelector, itemContainerSelector, etc.
  - Can be loaded from application.properties via @ConfigurationProperties so selectors
    are externally configurable without recompiling — useful when crawling different sites.
  - Can also be passed in dynamically as part of a CrawlRequestDTO if the UI allows
    users to specify custom selectors for arbitrary sites.

  WHY IT EXISTS:
  Hardcoding selectors inside HtmlParserService would make the crawler rigid — tied
  to one site forever. Externalizing selectors into this config class turns the crawler
  into a general-purpose extraction engine that works on any site with the right config.

  CONNECTS TO:
  - HtmlParserService reads selector strings from this class to know what to look for.
  - CrawlRequestDTO may carry a SelectorConfig when the user supplies custom selectors.
  - application.properties can define default selector values mapped here via Spring's
    @ConfigurationProperties binding.
*/
