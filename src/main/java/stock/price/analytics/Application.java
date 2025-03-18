package stock.price.analytics;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import stock.price.analytics.cache.CacheInitializationService;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.service.PricesService;
import stock.price.analytics.service.StockService;

import java.time.LocalDate;
import java.util.List;

import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.TradingDateUtil.isFirstImportFor;

@RequiredArgsConstructor
@SpringBootApplication
public class Application implements ApplicationRunner {

    private final StockRepository stockRepository;
    private final StockService stockService;
    private final PricesService pricesService;
    private final CacheInitializationService cacheInitializationService;
    private final CacheService cacheService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            boolean firstImportFor = pricesService.isFirstImportFor(timeframe);
            System.out.println(timeframe + " isFirstImport: " + firstImportFor);
            cacheInitializationService.setFirstImportFor(timeframe, firstImportFor);
        }

        List<Stock> stocks = stockRepository.findByXtbStockIsTrueAndDelistedDateIsNull();
        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            logTime(() -> cacheInitializationService.initHigherTimeframePricesCache(pricesService.previousThreePricesFor(tickers, timeframe)), "initialized " + timeframe + " prices cache");
        }
        logTime(() -> cacheInitializationService.initializeStocks(stocks), "initialized xtb stocks cache");
        LocalDate latestDailyPriceImportDate = stockService.findLastUpdate(); // find last update from stocksCache
        logTime(() -> cacheInitializationService.initHighLowPricesCache(latestDailyPriceImportDate), "initialized high low prices cache");
        if (isFirstImportFor(StockTimeframe.WEEKLY, latestDailyPriceImportDate) &&  cacheService.isFirstImportFor(StockTimeframe.WEEKLY)) {
            stockService.updateHighLowForPeriodFromHLCachesAndAdjustWeekend();
        }
        logTime(cacheInitializationService::initLatestDailyPricesCache, "initialized latest daily prices cache");
        logTime(cacheInitializationService::initDailyJSONPricesCache, "initialized daily JSON prices cache");
        logTime(cacheInitializationService::initializePreMarketDailyPrices, "initialized pre-market daily prices cache");
    }
}
