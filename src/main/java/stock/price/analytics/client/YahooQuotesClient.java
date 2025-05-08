package stock.price.analytics.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
    private static final int MAX_RETRIES_CRUMB = 5;
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

    private final RestTemplate restTemplate;
    private int RETRY_COUNT_CRUMB = 0;
    private String COOKIE_FC_YAHOO = "EuConsent=CQONUAAQONUAAAOACKROBgFgAAAAAAAAACiQAAAAAAAA; A1S=d=AQABBHu40mcCECI3dDTimCiMyiAvT34B1oUFEgABCAH-02cCaPF3ziMAAiAAAAcIdrfSZ2r5DG4&S=AQAAAszl9s9YvJYIkeFU0a_zZTg; A1=d=AQABBHu40mcCECI3dDTimCiMyiAvT34B1oUFEgABCAH-02cCaPF3ziMAAiAAAAcIdrfSZ2r5DG4&S=AQAAAszl9s9YvJYIkeFU0a_zZTg; GUC=AQABCAFn0_5oAkIdNgR3&s=AQAAAL3PAraG&g=Z9K4hQ; A3=d=AQABBHu40mcCECI3dDTimCiMyiAvT34B1oUFEgABCAH-02cCaPF3ziMAAiAAAAcIdrfSZ2r5DG4&S=AQAAAszl9s9YvJYIkeFU0a_zZTg; PRF=theme%3Dauto";
    private String CRUMB_COOKIE = "Ux/F/1Q/D1k";

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
        String quoteResponse = null;
        int maxRetries = 3;
        int retryCount = 0;

        log.info("quotePricesJSON called {} {}", CRUMB_COOKIE, COOKIE_FC_YAHOO);

        while (quoteResponse == null || quoteResponse.isEmpty()) {
            if (retryCount >= maxRetries) {
                log.error("Failed to retrieve quote after {} retries.", maxRetries);
                break; // Exit loop if max retries reached
            }

            try {
                HttpGet request = new HttpGet(URL);

                request.setHeader("Cookie", COOKIE_FC_YAHOO);
                request.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);

                HttpResponse response = httpClient.execute(request);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    quoteResponse = EntityUtils.toString(entity);
                    if (quoteResponse != null && !quoteResponse.contains("\"error\":null")) {
                        log.warn("ERROR for URL {}", URL);
                        log.warn("quoteResponse {}", quoteResponse);
                    }
                } else {
                    log.info("Empty response received. Retrying...");
                    retryCount++;
                    Thread.sleep(1000); // Wait for 1 second before retrying
                }
            } catch (IOException | InterruptedException e) {
                log.error(e.getMessage());
                retryCount++;
                try {
                    Thread.sleep(1000); // Wait before retrying
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return quoteResponse;
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

    private String getCrumb() {
        if (!CRUMB_COOKIE.isBlank()) {
            return CRUMB_COOKIE;
        }
        while (RETRY_COUNT_CRUMB < MAX_RETRIES_CRUMB) {
            String crumb = null;
            try {
                HttpGet request = new HttpGet(QUERY2_BASE_URL + "/v1/test/getcrumb");
                request.setHeader("Cookie", cookieFromFcYahoo());
                request.setHeader("User-Agent", USER_AGENT_VALUE);

                HttpResponse response = httpClient.execute(request);

                int statusCode = response.getStatusLine().getStatusCode();
                if (HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        crumb = EntityUtils.toString(entity);
                        CRUMB_COOKIE = crumb;
                    }
                } else {
                    RETRY_COUNT_CRUMB++;
                    log.warn("Non-2xx status code received for crumb. Retrying ({}/{})...", RETRY_COUNT_CRUMB, MAX_RETRIES_CRUMB);
                    Thread.sleep(1000L * RETRY_COUNT_CRUMB); // Add a backoff delay
                    continue;
                }
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("crumb: {}", CRUMB_COOKIE);
            return crumb;
        }
        // If we reach this point, all retries have been exhausted
        log.error("Maximum number of retries reached. Unable to get crumb.");
        throw new RuntimeException("Unable to get crumb after multiple attempts.");
    }

    public void getAllHistoricalPrices_andSaveJSONFileFor(String tickers) {
        int lowerBound = 100;
        int upperBound = 200;
        int range = (upperBound - lowerBound) + 1;
        try {
            for (String ticker : tickers.split(",")) {
                long currentTime = System.currentTimeMillis();
                ResponseEntity<String> response;
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Cookie", cookieFromFcYahoo());
                    headers.add(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);

                    org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(null, headers);
                    response = restTemplate.exchange(
                            QUERY1_BASE_URL + V7_FINANCE + "/chart/{ticker}?range=60y&interval=1d&indicators=quote&includeTimestamps=true",
                            HttpMethod.GET,
                            entity,
                            String.class,
                            ticker
                    );

                    String responseBody = response.getBody();
                    if (responseBody != null) {
                        String fileName = ticker + ".json";
                        String path = "./all-historical-prices/DAILY/" + fileName;
                        writeToFile(path, responseBody);
                    } else {
                        log.error("response body is null for ticker {}", ticker);
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
