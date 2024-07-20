package stock.price.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.DailyPriceOHLCRepository;
import stock.price.analytics.service.PriceOHLCService;
import stock.price.analytics.service.YahooQuoteService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class Application implements ApplicationRunner {

    private final YahooQuoteService yahooQuoteService;
    private final PriceOHLCService priceOHLCService;
    private final DailyPriceOHLCRepository dailyPriceOHLCRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<DailyPriceOHLC> all_daily_prices = dailyPriceOHLCRepository.findXTBLatestByTickerWithDateAfter(LocalDate.of(2024, 7, 1));
        if (all_daily_prices.isEmpty()) {
            List<String> fileNames = List.of("12-07-2024_1", "12-07-2024_2", "12-07-2024_3", "12-07-2024_4",
                    "15-07-2024_1",
                    "15-07-2024_2",
                    "16-07-2024_1",
                    "16-07-2024_2",
                    "17-07-2024_1",
                    "17-07-2024_2",
                    "18-07-2024_1",
                    "18-07-2024_2",
                    "19-07-2024_1",
                    "19-07-2024_2"
            );
            for (String fileName : fileNames) {
                log.info("initializing app with some data from filename : {}", fileName);
                List<DailyPriceOHLC> importedDailyPrices = yahooQuoteService.dailyPricesFromFile(fileName);
                if (!importedDailyPrices.isEmpty()) {
                    priceOHLCService.updatePricesForHigherTimeframes(importedDailyPrices);
                } else {
                    log.info("importedDailyPrices empty");
                }
                log.info("imported {}", importedDailyPrices.size());
            }
        } else {
            log.info("all_daily_prices size : {}", all_daily_prices.size());
        }
    }
}
