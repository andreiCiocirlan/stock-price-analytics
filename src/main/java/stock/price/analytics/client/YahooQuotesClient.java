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
            HttpGet request = new HttpGet(URL);
            request.setHeader("Cookie", COOKIE_FC_YAHOO);
            request.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);

            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new RuntimeException("Empty response entity received");
            }

            String quoteResponse = EntityUtils.toString(entity);
            if (quoteResponse == null || quoteResponse.isEmpty()) {
                throw new RuntimeException("Empty response body received");
            }

            if (statusCode != 200) {
                log.warn("Non-200 status {} for URL {}", statusCode, URL);
                log.warn("Response: {}", quoteResponse);
                throw new RuntimeException(String.format("HTTP %d: %s", statusCode, quoteResponse));
            }

            return quoteResponse;
        } catch (IOException e) {
            log.error("IO error fetching quotes: {}", e.getMessage());
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
            HttpGet request = new HttpGet(url);
            request.setHeader("Cookie", cookieFromFcYahoo());
            request.setHeader("User-Agent", USER_AGENT_VALUE);

            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                String body = EntityUtils.toString(response.getEntity());
                throw new RuntimeException(String.format("HTTP %d from crumb endpoint: %s", statusCode, body));
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new RuntimeException("Empty response entity from crumb endpoint");
            }

            String crumb = EntityUtils.toString(entity).trim();
            if (crumb.isEmpty()) {
                throw new RuntimeException("Empty crumb response body");
            }

            CRUMB_COOKIE = crumb;
            log.info("Successfully retrieved crumb: {}", CRUMB_COOKIE);
            return crumb;

        } catch (IOException e) {
            log.error("IO error fetching crumb: {}", e.getMessage());
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
                ResponseEntity<String> response;
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Cookie", COOKIE_FC_YAHOO);
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
                        writeToFile("./all-historical-prices/DAILY/" + ticker + ".json", responseBody);
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
