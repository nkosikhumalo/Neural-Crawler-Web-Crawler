
/*
  FILE: ExportService.java
  ==========================
  Service class responsible for serializing extracted crawl results into
  downloadable file formats (CSV and JSON).

  WHAT IT DOES:
  - Takes the in-memory list of CrawlResult / BookItem domain objects and converts
    them into a CSV byte stream using OpenCSV, writing column headers automatically
    from field names.
  - Converts the same data into a formatted JSON byte stream using Jackson ObjectMapper.
  - Returns byte arrays or InputStreams that the controller wraps into HTTP download
    responses with correct Content-Disposition and Content-Type headers.
  - Handles edge cases: empty result sets, special characters in data, null fields.

  WHY IT EXISTS:
  Export logic is isolated here so it can be reused by multiple endpoints and tested
  independently. If a new export format (e.g., XML, Excel) is needed later, only this
  file needs updating.

  CONNECTS TO:
  - CrawlerService calls this when an export is requested.
  - CrawlController passes the result back to the HTTP response.
  - BookItem / CrawlResult models are the input data this service serializes.
  - pom.xml must have OpenCSV and Jackson dependencies declared.
*/
