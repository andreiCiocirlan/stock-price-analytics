package stock.price.analytics;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.service.*;
import stock.price.analytics.util.LoggingUtil;

@RequiredArgsConstructor
@SpringBootApplication
public class Application implements ApplicationRunner {

    private final StockRepository stockRepository;
    private final StockService stockService;
    private final HigherTimeframePricesCacheService higherTimeframePricesCacheService;
    private final HighLowPricesCacheService highLowPricesCacheService;
    private final DailyPricesCacheService dailyPricesCacheService;
    private final DailyPricesJSONCacheService dailyPricesJSONCacheService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LoggingUtil.logTime(stockRepository::updateIpoAndDelistedDates, "updated ipo/delisted dates at start-up");
        LoggingUtil.logTime(higherTimeframePricesCacheService::initHigherTimeframePricesCache, "initialized higher-timeframe prices cache");
        LoggingUtil.logTime(stockService::initStocksCache, "initialized xtb stocks cache");
        LoggingUtil.logTime(highLowPricesCacheService::initHighLowPricesCache, "initialized high low prices cache");
        LoggingUtil.logTime(dailyPricesCacheService::initDailyPricesCache, "initialized daily prices cache");
        LoggingUtil.logTime(dailyPricesJSONCacheService::initDailyJSONPricesCache, "initialized daily JSON prices cache");
    }
}
