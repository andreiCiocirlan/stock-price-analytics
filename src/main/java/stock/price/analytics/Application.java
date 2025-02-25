package stock.price.analytics;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import stock.price.analytics.cache.HighLowPricesCacheService;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.service.DailyPricesJSONService;
import stock.price.analytics.service.DailyPricesService;
import stock.price.analytics.service.PricesService;
import stock.price.analytics.service.StockService;

import java.time.LocalDate;
import java.util.List;

import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.TradingDateUtil.isFirstImportMonday;

@RequiredArgsConstructor
@SpringBootApplication
public class Application implements ApplicationRunner {

    private final StockRepository stockRepository;
    private final StockService stockService;
    private final PricesService pricesService;
    private final HighLowPricesCacheService highLowPricesCacheService;
    private final DailyPricesService dailyPricesService;
    private final DailyPricesJSONService dailyPricesJSONService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> tickers = stockRepository.findByXtbStockTrueAndDelistedDateIsNull().stream().map(Stock::getTicker).toList();
        logTime(() -> pricesService.initHigherTimeframePricesCache(tickers), "initialized higher-timeframe prices cache");
        logTime(stockService::initStocksCache, "initialized xtb stocks cache");
        LocalDate latestDailyPriceImportDate = stockService.findLastUpdate(); // find last update from stocksCache
        logTime(() -> highLowPricesCacheService.initHighLowPricesCache(latestDailyPriceImportDate), "initialized high low prices cache");
        if (isFirstImportMonday(latestDailyPriceImportDate)) {
            stockService.updateHighLowForPeriodFromHLCachesAndAdjustWeekend();
        }
        logTime(dailyPricesService::initLatestTwoDaysPricesCache, "initialized latest two days prices cache");
        logTime(dailyPricesJSONService::initDailyJSONPricesCache, "initialized daily JSON prices cache");
        logTime(dailyPricesService::initPreMarketDailyPricesCache, "initialized pre-market daily prices cache");
    }
}
