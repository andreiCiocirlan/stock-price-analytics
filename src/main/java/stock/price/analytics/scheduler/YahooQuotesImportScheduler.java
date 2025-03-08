package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooQuotesImportScheduler {

    public static final String HTTP_LOCALHOST = "http://localhost:";
    public static final String YAHOO_QUOTES_IMPORT_ENDPOINT = "/yahoo-quotes/import";
    public static final String INTRADAY_LOG_PREFIX = "INTRADAY";
    public static final String PREMARKET_LOG_PREFIX = "PRE-MARKET";

    private final RestTemplate restTemplate;
    @Value("${server.port}")
    private String serverPort;

    // executed at 9:35 NY time (5 minutes after market open)
    // executed at 5 and 35 minutes past the hour between 10-17 NY time (intraday)
    @Schedules({
            @Scheduled(cron = "${cron.yahoo.quotes.intraday.at935}", zone = "${cron.timezone}"),
            @Scheduled(cron = "${cron.yahoo.quotes.intraday.between10and16}", zone = "${cron.timezone}"),
            @Scheduled(cron = "${cron.yahoo.quotes.intraday.between16and17}", zone = "${cron.timezone}")
    })
    public void yahooQuotesIntraday() {
        callYahooQuotesImport(INTRADAY_LOG_PREFIX);
    }

    // executed at 8:00, 8:15, 8:30, 8:45 NY time (pre-market)
    // executed at 9:00, 9:15 NY time (pre-market)
    @Schedules({
            @Scheduled(cron = "${cron.yahoo.quotes.pre.market.between8and9}", zone = "${cron.timezone}"),
            @Scheduled(cron = "${cron.yahoo.quotes.pre.market.between9and915}", zone = "${cron.timezone}")
    })
    public void yahooQuotesPreMarket() {
        callYahooQuotesImport(PREMARKET_LOG_PREFIX);
    }

    private void callYahooQuotesImport(String logPrefix) {
        restTemplate.getForObject(String.join("", HTTP_LOCALHOST, serverPort, YAHOO_QUOTES_IMPORT_ENDPOINT), List.class);
        log.info(logPrefix + " Yahoo Quotes Scheduler imported prices successfully");
    }

}