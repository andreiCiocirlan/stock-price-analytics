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
import stock.price.analytics.util.LoggingUtil;

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
        LoggingUtil.logTime(higherTimeframePricesCacheService::initHigherTimeframePricesCache, "initialized higher-timeframe prices cache");
        LoggingUtil.logTime(stockService::initStocksCache, "initialized xtb stocks cache");
        LoggingUtil.logTime(highLowPricesCacheService::initHighLowPricesCache, "initialized high low prices cache");
        LoggingUtil.logTime(dailyPricesService::initDailyPricesCache, "initialized daily prices cache");
        LoggingUtil.logTime(dailyPricesJSONService::initDailyJSONPricesCache, "initialized daily JSON prices cache");
        LoggingUtil.logTime(dailyPricesService::initPreMarketDailyPricesCache, "initialized pre-market daily prices cache");
        LoggingUtil.logTime(stockRepository::updateIpoAndDelistedDates, "updated ipo/delisted dates at start-up");
    }
}
