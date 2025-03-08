package stock.price.analytics;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import stock.price.analytics.cache.CacheInitializationService;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.json.DailyPricesJSON;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.DailyPricesJSONRepository;
import stock.price.analytics.repository.prices.DailyPricesRepository;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.service.PricesService;
import stock.price.analytics.service.StockService;

import java.time.LocalDate;
import java.util.List;

import static stock.price.analytics.util.ImportDateUtil.isFirstImportFor;
import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@RequiredArgsConstructor
@SpringBootApplication
public class Application implements ApplicationRunner {

    private final StockRepository stockRepository;
    private final DailyPricesJSONRepository dailyPricesJSONRepository;
    private final DailyPricesRepository dailyPricesRepository;
    private final StockService stockService;
    private final PricesService pricesService;
    private final CacheInitializationService cacheInitializationService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Stock> stocks = stockRepository.findByXtbStockIsTrueAndDelistedDateIsNull();
        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            logTime(() -> cacheInitializationService.initHigherTimeframePricesCache(pricesService.previousThreePricesFor(tickers, timeframe)), "initialized " + timeframe + " prices cache");
        }
        logTime(() -> cacheInitializationService.initializeStocks(stocks), "initialized xtb stocks cache");
        LocalDate latestDailyPriceImportDate = stockService.findLastUpdate(); // find last update from stocksCache
        logTime(() -> cacheInitializationService.initHighLowPricesCache(latestDailyPriceImportDate), "initialized high low prices cache");
        if (isFirstImportFor(StockTimeframe.WEEKLY, latestDailyPriceImportDate)) {
            stockService.updateHighLowForPeriodFromHLCachesAndAdjustWeekend();
        }
        logTime(() -> cacheInitializationService.initLatestTwoDaysPricesCache(dailyPricesRepository.findLatestTwoDailyPrices()), "initialized latest two days prices cache");
        List<DailyPricesJSON> latestDailyPricesJSON = dailyPricesJSONRepository.findByDateBetween(tradingDateNow().minusDays(7), tradingDateNow());
        logTime(() -> cacheInitializationService.initDailyJSONPricesCache(latestDailyPricesJSON), "initialized daily JSON prices cache");
        logTime(cacheInitializationService::initializePreMarketDailyPrices, "initialized pre-market daily prices cache");
    }
}
