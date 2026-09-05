
/*
  FILE: HtmlParserServiceTest.java
  ==================================
  Unit test class for HtmlParserService — verifies that CSS selector logic correctly
  extracts data from HTML without making any real network requests.

  WHAT IT TESTS:
  - That a sample books.toscrape.com HTML fixture (a saved .html file in test/resources)
    is correctly parsed into a list of BookItem objects with the right title, price,
    and availability values.
  - That missing elements (e.g., a book without a price) produce null/empty fields
    rather than throwing NullPointerExceptions.
  - That pagination link extraction correctly returns the "next" page href.
  - That relative URLs are resolved correctly against the base URL.
  - Edge cases: empty HTML string, malformed HTML, page with zero results.

  WHY IT EXISTS:
  Parser logic is the most fragile part of a crawler — a one-character selector change
  can silently break all data extraction. Tests here catch regressions immediately
  without needing to run the full application or make live HTTP requests.

  CONNECTS TO:
  - HtmlParserService is the class under test — instantiated directly or injected.
  - Test fixture HTML files in src/test/resources/ provide realistic input data.
  - SelectorConfig is configured with test selector values.
  - JUnit 5 + AssertJ (or plain JUnit assertions) are the test framework dependencies.
*/
