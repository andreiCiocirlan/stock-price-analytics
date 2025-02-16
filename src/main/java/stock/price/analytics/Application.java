package stock.price.analytics;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.service.*;

import static stock.price.analytics.util.LoggingUtil.logTime;

@RequiredArgsConstructor
@SpringBootApplication
@EnableScheduling
public class Application implements ApplicationRunner {

    private final StockRepository stockRepository;
    private final StockService stockService;
    private final HigherTimeframePricesCacheService higherTimeframePricesCacheService;
    private final HighLowPricesCacheService highLowPricesCacheService;
    private final DailyPricesService dailyPricesService;
    private final DailyPricesJSONService dailyPricesJSONService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        logTime(higherTimeframePricesCacheService::initHigherTimeframePricesCache, "initialized higher-timeframe prices cache");
        logTime(stockService::initStocksCache, "initialized xtb stocks cache");
        logTime(highLowPricesCacheService::initHighLowPricesCache, "initialized high low prices cache");
        logTime(dailyPricesService::initLatestTwoDaysPricesCache, "initialized latest two days prices cache");
        logTime(dailyPricesJSONService::initDailyJSONPricesCache, "initialized daily JSON prices cache");
        logTime(dailyPricesService::initPreMarketDailyPricesCache, "initialized pre-market daily prices cache");
        logTime(stockRepository::updateIpoAndDelistedDates, "updated ipo/delisted dates at start-up");
    }
}
