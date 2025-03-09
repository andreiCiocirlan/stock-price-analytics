package stock.price.analytics.client.yahoo;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooQuoteClient {

    private static final String USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0";
    private static final int MAX_RETRIES_CRUMB = 5;
    private int RETRY_COUNT_CRUMB = 0;
    private String COOKIE_FC_YAHOO = "";
    private String CRUMB_COOKIE = "";

    public String quotePricesJSON(String tickers) {
        String URL = String.join("", "https://query2.finance.yahoo.com/v7/finance/quote?lang=en-US&region=US&corsDomain=finance.yahoo.com&symbols=",
                tickers, "&crumb=", getCrumb());
        String quoteReponse = null;
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(URL);

            request.setHeader("Cookie", COOKIE_FC_YAHOO);
            request.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);

            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                quoteReponse = EntityUtils.toString(entity);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return quoteReponse;
    }

    public String cookieFromFcYahoo() {
        String cookieValue = null;
        try (CloseableHttpClient httpClient = createHttpClient()) {
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
            try (CloseableHttpClient httpClient = createHttpClient()) {
                HttpGet request = new HttpGet("https://query2.finance.yahoo.com/v1/test/getcrumb");
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

    private CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
