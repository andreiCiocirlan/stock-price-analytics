package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static stock.price.analytics.util.Constants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooQuotesImportScheduler {

    private final RestTemplate restTemplate;
    @Value("${server.port}")
    private String serverPort;

    @Schedules({
            @Scheduled(cron = "${cron.yahoo.quotes.intraday.at935}", zone = "${cron.timezone}"),            // 0 35,55 9 * * MON-FRI
            @Scheduled(cron = "${cron.yahoo.quotes.intraday.between10and16}", zone = "${cron.timezone}"),   // 0 15,35,55 10-15 * * MON-FRI
            @Scheduled(cron = "${cron.yahoo.quotes.intraday.between16and17}", zone = "${cron.timezone}")    // 0 15,35 16 * * MON-FRI
    })
    public void yahooQuotesIntraday() {
        callYahooQuotesImport(INTRADAY_LOG_PREFIX);
    }

    @Schedules({
            @Scheduled(cron = "${cron.yahoo.quotes.pre.market.between8and9}", zone = "${cron.timezone}"),   // 0 15,30,45 8 * * MON-FRI
            @Scheduled(cron = "${cron.yahoo.quotes.pre.market.between9and915}", zone = "${cron.timezone}")  // 0 0,15 9 * * MON-FRI
    })
    public void yahooQuotesPreMarket() {
        callYahooQuotesImport(PREMARKET_LOG_PREFIX);
    }

    private void callYahooQuotesImport(String logPrefix) {
        restTemplate.getForObject(String.join("", HTTP_LOCALHOST, serverPort, YAHOO_QUOTES_IMPORT_ENDPOINT), List.class);
        log.info(logPrefix + " Yahoo Quotes Scheduler imported prices successfully");
    }

}