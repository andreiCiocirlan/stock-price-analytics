package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockRepository;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final CacheService cacheService;
    private final AsyncPersistenceService asyncPersistenceService;

    @Transactional
    public void saveStocks(String tickers, boolean xtbStock, boolean shortSell, double cfdMargin) {
        for (String ticker : tickers.split(",")) {
            if (!cacheService.getStocksMap().containsKey(tickers)) { // only create new stock object if not already in DB
                stockRepository.save(new Stock(ticker, xtbStock, shortSell, cfdMargin));
            }
        }
    }

    private void updateStocksFromOHLCPrices(List<DailyPrice> dailyPrices, List<AbstractPrice> htfPrices, Set<Stock> stocksUpdated) {
        Map<String, Stock> stocksMap = cacheService.getStocksMap();
        // update from daily prices
        for (DailyPrice dailyPrice : dailyPrices) {
            String ticker = dailyPrice.getTicker();
            Optional.ofNullable(stocksMap.get(ticker))
                    .filter(stock -> !stock.getLastUpdated().isAfter(dailyPrice.getDate()))
                    .ifPresent(stock -> {
                        stock.updateFrom(dailyPrice);
                        stocksUpdated.add(stock);
                    });
        }

        // update from higher timeframe prices
        for (AbstractPrice wmyPrice : htfPrices) {
            String ticker = wmyPrice.getTicker();
            Stock stock = stocksMap.getOrDefault(ticker, new Stock(ticker, wmyPrice.getStartDate(), true));
            stock.updateFrom(wmyPrice);
            stocksUpdated.add(stock);
        }
    }

    private void updateStocksFromHighLowCaches(Set<Stock> stocksUpdated) {
        Map<String, Stock> stocksMap = cacheService.getStocksMap();

        for (HighLowPeriod period : HighLowPeriod.values()) {
            List<? extends HighLowForPeriod> cache = cacheService.highLowForPeriodPricesFor(period);
            for (HighLowForPeriod hl : cache) {
                Optional.ofNullable(stocksMap.get(hl.getTicker()))
                        .ifPresent(stock -> {
                            stock.updateFrom(hl);
                            stocksUpdated.add(stock);
                        });
            }
        }
    }

    @Transactional
    public void updateStocksHighLowsAndOHLCFrom(List<DailyPrice> dailyPrices, List<AbstractPrice> htfPrices) {
        Set<Stock> stocksUpdated = new HashSet<>();
        updateStocksFromOHLCPrices(dailyPrices, htfPrices, stocksUpdated);
        updateStocksFromHighLowCaches(stocksUpdated);

        List<Stock> stocks = new ArrayList<>(stocksUpdated);
        asyncPersistenceService.partitionDataAndSaveWithLogTime(stocks, stockRepository, "saved " + stocks.size() + " stocks after OHLC higher-timeframe and high-lows 4w, 52w, all-time updates");
        cacheService.addStocks(stocks);
    }

    public void updateHighLowForPeriodFromHLCachesAndAdjustWeekend() {
        Set<Stock> stocksUpdated = new HashSet<>();
        updateStocksFromHighLowCaches(stocksUpdated);

        List<Stock> stocks = new ArrayList<>(stocksUpdated);
        asyncPersistenceService.partitionDataAndSaveWithLogTime(stocks, stockRepository, "saved stocks after generating high-lows 4w, 52w, all-time for the first import of the week");
        cacheService.addStocks(stocks);
    }

    public LocalDate findLastUpdate() {
        return cacheService.getStocksMap().values().stream()
                .max(Comparator.comparing(Stock::getLastUpdated))
                .map(Stock::getLastUpdated)
                .orElseThrow();
    }

    public void updateStockDailyPricesFor(String ticker) {
        stockRepository.updateStockDailyPricesFor(ticker);
    }

    public void updateStockHigherTimeframePricesFor(String ticker) {
        stockRepository.updateStockWeeklyPricesFor(ticker);
        stockRepository.updateStockMonthlyPricesFor(ticker);
        stockRepository.updateStockQuarterlyPricesFor(ticker);
        stockRepository.updateStockYearlyPricesFor(ticker);
    }

    public void updateHighLowForPeriodPrices(String ticker) {
        stockRepository.updateHighLow4wPricesFor(ticker);
        stockRepository.updateHighLow52wPricesFor(ticker);
        stockRepository.updateHighestLowestPricesFor(ticker);
    }
}
