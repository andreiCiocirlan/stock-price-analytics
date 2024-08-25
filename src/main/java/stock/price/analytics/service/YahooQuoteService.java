package stock.price.analytics.service;

import jakarta.transaction.Transactional;
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
import org.springframework.stereotype.Service;
import stock.price.analytics.client.yahoo.YahooFinanceClient;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static stock.price.analytics.config.TradingDateUtil.tradingDateNow;

@Slf4j
@Service
@RequiredArgsConstructor
public class YahooQuoteService {

    private static final String USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0";
    private static final int MAX_RETRIES_CRUMB = 5;
    private final YahooFinanceClient yahooFinanceClient;
    private final DailyPriceOHLCService dailyPriceOHLCService;
    private int RETRY_COUNT_CRUMB = 0;
    private String COOKIE_FC_YAHOO = "";
    private String CRUMB_COOKIE = "";

    public List<DailyPriceOHLC> dailyPricesFromFile(String fileName) {
        return yahooFinanceClient.dailyPricesFromFile(fileName);
    }

    public List<DailyPriceOHLC> dailyPricesImport() {
        return dailyPricesImport(false);
    }

    @Transactional
    public List<DailyPriceOHLC> dailyPricesImport(boolean preMarketOnly) {
        int maxTickersPerRequest = 1650;
        List<DailyPriceOHLC> dailyImportedPrices = new ArrayList<>();
        LocalDate minTradingDate = tradingDateNow().minusDays(5); // max 5 calendar days in the past for previous intraDay prices to be found
        List<DailyPriceOHLC> latestByTicker = dailyPriceOHLCService.findXTBLatestByTickerWithDateAfter(minTradingDate);

        int start = 0;
        int end = Math.min(maxTickersPerRequest, latestByTicker.size());
        int fileCounter = 1;
        while (start < latestByTicker.size()) {
            List<DailyPriceOHLC> partition = latestByTicker.subList(start, end);
            String tickers = partition.stream().map(DailyPriceOHLC::getTicker).collect(Collectors.joining(","));
            String pricesJSON = quotePricesJSON(tickers, getCrumb());

            List<DailyPriceOHLC> dailyPriceOHLCs = yahooFinanceClient.extractDailyPricesFromJSON(pricesJSON, preMarketOnly);
            List<DailyPriceOHLC> dailyPrices = dailyPriceOHLCService.getDailyImportedPrices(dailyPriceOHLCs, latestByTicker.stream()
                    .collect(Collectors.toMap(DailyPriceOHLC::getTicker, p -> p)));
            dailyImportedPrices.addAll(dailyPrices);

            if (!preMarketOnly || dailyPriceOHLCs.isEmpty()) {
                String fileName = dailyPriceOHLCs.getFirst().getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "_" + fileCounter + ".json";
                String path = "./yahoo-daily-prices/" + fileName;
                writeToFile(path, pricesJSON);
            }

            start = end;
            end = Math.min(start + maxTickersPerRequest, latestByTicker.size());
            fileCounter++;
        }

        if (!preMarketOnly || dailyImportedPrices.isEmpty()) { // only save if intraday prices, for pre-market only display
            dailyPriceOHLCService.saveDailyPrices(dailyImportedPrices);
        }
        return dailyImportedPrices;
    }

    private void writeToFile(String filePath, String jsonData) {
        try {
            File jsonFile = new File(filePath);

            try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
                outputStream.write(jsonData.getBytes(StandardCharsets.UTF_8));
            }
            log.info("saved daily prices file {}", jsonFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error writing to file: {}", filePath, e);
        }
    }

    private String quotePricesJSON(String tickers, String crumb) {
        String URL = String.join("", "https://query2.finance.yahoo.com/v7/finance/quote?lang=en-US&region=US&corsDomain=finance.yahoo.com&symbols=",
                tickers, "&crumb=", crumb);
        String quoteReponse = null;
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(URL);

            request.setHeader("Cookie", COOKIE_FC_YAHOO);
            request.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0");

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

    public String getCrumb() {
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
