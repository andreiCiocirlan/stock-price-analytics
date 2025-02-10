package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntradayYahooPricesScheduler {

    @Value("${server.port}")
    private int serverPort;

    private final RestTemplate restTemplate;

    // Get Yahoo-API prices at 5 minutes past every hour from 9 AM to 4 PM (NY time) MON to FRI
    @Scheduled(cron = "${cron.expression.intraday.yahoo.quotes}", zone = "${cron.expression.timezone}")
    public void getYahooPrices() {
        String url = "http://localhost:" + serverPort + "/yahoo-prices/import"; // Use the injected port
        ResponseEntity<List<DailyPrice>> response = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        log.info("Intraday Yahoo Prices Scheduler imported: {} daily prices", Objects.requireNonNull(response.getBody()).size());
    }
}