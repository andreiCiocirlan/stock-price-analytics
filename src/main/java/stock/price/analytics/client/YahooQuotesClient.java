package stock.price.analytics.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import stock.price.analytics.util.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static stock.price.analytics.util.Constants.USER_AGENT_VALUE;
import static stock.price.analytics.util.FileUtil.writeToFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooQuotesClient {

    private static final CloseableHttpClient httpClient;
    private static final String QUERY1_BASE_URL = "https://query1.finance.yahoo.com";
    private static final String QUERY2_BASE_URL = "https://query2.finance.yahoo.com";
    private static final String V7_FINANCE = "/v7/finance";

    static {
        httpClient = createHttpClient();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                log.error("Error closing HttpClient", e);
            }
        }));
    }

    private final RestClient restClient;
    private String COOKIE_FC_YAHOO = "A3=d=AQABBOnlcGkCEHdoCk2-i8zNajDWzRMSlfcFEgABAQEpcml6afF3ziMAAAAAgA&S=AQAAAgHfzgCRNADaQ3YuHkqHRts";
    private String CRUMB_COOKIE = "ztjAVP7wZ61";

    private static CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public List<String> quotePricesFor(List<String> tickers) {
        int maxTickersPerRequest = 1700;

        List<String> res = new ArrayList<>();
        List<String> partitions = new ArrayList<>();
        int start = 0;
        int end = Math.min(maxTickersPerRequest, tickers.size());
        while (start < tickers.size()) {
            List<String> partition = tickers.subList(start, end);
            String tickersFormatted = String.join(",", partition);
            partitions.add(tickersFormatted);

            start = end;
            end = Math.min(start + maxTickersPerRequest, tickers.size());
        }

        partitions.parallelStream().forEachOrdered(s -> res.add(quotePricesJSON(s)));
        return res;
    }

    public String quotePricesJSON(String tickers) {
        String crumb = CRUMB_COOKIE.isEmpty() ? getCrumb() : CRUMB_COOKIE;
        String URL = String.join("", QUERY2_BASE_URL + V7_FINANCE + "/quote?lang=en-US&region=US&corsDomain=finance.yahoo.com&symbols=",
                tickers, "&crumb=", crumb);

        log.info("quotePricesJSON called {} {}", crumb, COOKIE_FC_YAHOO);

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(URL)
                    .header("Cookie", COOKIE_FC_YAHOO)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT_VALUE)
                    .retrieve()
                    .toEntity(String.class);

            String quoteResponse = response.getBody();
            int statusCode = response.getStatusCode().value();
            if (quoteResponse == null || quoteResponse.isEmpty()) {
                throw new RuntimeException("Empty response body received");
            }

            if (statusCode != 200) {
                log.warn("Non-200 status {} for URL {}", statusCode, URL);
                log.warn("Response: {}", quoteResponse);
                throw new RuntimeException(String.format("HTTP %d: %s", statusCode, quoteResponse));
            }

            return quoteResponse;
        } catch (RestClientException e) {
            log.error("Error fetching quotes: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch quotes from Yahoo Finance", e);
        }
    }

    public String cookieFromFcYahoo() {
        String cookieValue = null;
        try {
            HttpGet request = new HttpGet("https://fc.yahoo.com");
            request.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);

            HttpResponse response = httpClient.execute(request);

            Header[] headers = response.getHeaders("Set-Cookie");
            if (headers.length == 1) {
                cookieValue = headers[0].getValue().split(";")[0]; // extracts "A1=...."
                COOKIE_FC_YAHOO = cookieValue;
                log.info("cookieFromFcYahoo cookie: {}", COOKIE_FC_YAHOO);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return cookieValue;
    }

    public String getCrumb() {
        String url = QUERY2_BASE_URL + "/v1/test/getcrumb";

        log.info("Fetching crumb from {}", url);

        try {
            String crumb = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, cookieFromFcYahoo())
                    .header(HttpHeaders.USER_AGENT, Constants.USER_AGENT_VALUE)
                    .retrieve()
                    .body(String.class);

            if (crumb == null || crumb.trim().isEmpty()) {
                throw new RuntimeException("Empty crumb response body");
            }

            CRUMB_COOKIE = crumb.trim();
            log.info("Successfully retrieved crumb: {}", CRUMB_COOKIE);
            return CRUMB_COOKIE;
        } catch (RestClientException e) {
            log.error("Failed to fetch crumb: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch crumb from Yahoo Finance", e);
        }
    }

    public void getAllHistoricalPrices_andSaveJSONFileFor(String tickers) {
        int lowerBound = 100;
        int upperBound = 150;
        int range = (upperBound - lowerBound) + 1;
        try {
            for (String ticker : tickers.split(",")) {
                long currentTime = System.currentTimeMillis();
                try {
                    String responseBody = restClient.get()
                            .uri(QUERY1_BASE_URL + V7_FINANCE + "/chart/{ticker}", ticker)
                            .header(HttpHeaders.COOKIE, COOKIE_FC_YAHOO)
                            .header(HttpHeaders.USER_AGENT, USER_AGENT_VALUE)
                            .retrieve()
                            .body(String.class);

                    if (responseBody != null && !responseBody.trim().isEmpty()) {
                        writeToFile("./all-historical-prices/DAILY/" + ticker + ".json", responseBody);
                    } else {
                        log.error("Empty response body for ticker {}", ticker);
                    }
                } catch (RestClientException e) {
                    log.error("Failed retrieving prices data for ticker {}: {}", ticker, e.getMessage());
                }
                log.info("saving JSON historical prices for {} took {} ms", ticker, (System.currentTimeMillis() - currentTime));
                int sleepTime = (int) (Math.random() * range) + lowerBound;
                Thread.sleep(sleepTime);
                log.info("sleeping for {} ms", sleepTime);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
