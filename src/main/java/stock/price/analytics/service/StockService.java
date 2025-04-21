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
import stock.price.analytics.repository.stocks.TickerRenameRepository;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final TickerRenameRepository tickerRenameRepository;
    private final CacheService cacheService;
    private final AsyncPersistenceService asyncPersistenceService;
    private final SyncPersistenceService syncPersistenceService;

    private Set<String> updateStocksFromOHLCPrices(List<DailyPrice> dailyPrices, List<AbstractPrice> htfPrices) {
        Set<String> tickersUpdated = new HashSet<>();
        Map<String, Stock> stocksMap = cacheService.getStocksMap();

        // order matters (htf prices processed first because stock closing price gets updated only from daily prices)
        for (AbstractPrice wmyPrice : htfPrices) {
            String ticker = wmyPrice.getTicker();
            Optional.ofNullable(stocksMap.get(ticker))
                    .ifPresent(stock -> {
                        if (stock.needsUpdate(wmyPrice)) {
                            tickersUpdated.add(ticker);
                            stock.updateFrom(wmyPrice);
                        }
                    });
        }

        for (DailyPrice dailyPrice : dailyPrices) {
            String ticker = dailyPrice.getTicker();
            Optional.ofNullable(stocksMap.get(ticker))
                    .ifPresent(stock -> {
                        if (stock.needsUpdate(dailyPrice)) {
                            tickersUpdated.add(ticker);
                            stock.updateFrom(dailyPrice);
                        }
                    });
        }
        return tickersUpdated;
    }

    private Set<String> updateStocksFromHighLowCaches() {
        Set<String> tickersUpdated = new HashSet<>();
        Map<String, Stock> stocksMap = cacheService.getStocksMap();

        for (HighLowPeriod period : HighLowPeriod.values()) {
            List<? extends HighLowForPeriod> cache = cacheService.highLowForPeriodPricesFor(period);
            for (HighLowForPeriod hl : cache) {
                Optional.ofNullable(stocksMap.get(hl.getTicker()))
                        .ifPresent(stock -> {
                            if (stock.needsUpdate(hl)) {
                                tickersUpdated.add(hl.getTicker());
                                stock.updateFrom(hl);
                            }
                        });
            }
        }
        return tickersUpdated;
    }

    @Transactional
    public void updateStocksHighLowsAndOHLCFrom(List<DailyPrice> dailyPrices, List<AbstractPrice> htfPrices) {
        Set<String> tickersUpdated = new HashSet<>(updateStocksFromOHLCPrices(dailyPrices, htfPrices));
        tickersUpdated.addAll(updateStocksFromHighLowCaches());
        List<Stock> stocksUpdated = cacheService.getCachedStocks().stream().filter(stock -> tickersUpdated.contains(stock.getTicker())).toList();
        asyncPersistenceService.partitionDataAndSaveWithLogTime(stocksUpdated, stockRepository, "saved " + stocksUpdated.size() + " stocks after OHLC higher-timeframe and high-lows 4w, 52w, all-time updates");
    }

    @Transactional
    public void updateHighLowForPeriodFromHLCachesAndAdjustWeekend() {
        Set<String> tickersUpdated = new HashSet<>(updateStocksFromHighLowCaches());
        List<Stock> stocksUpdated = cacheService.getCachedStocks().stream().filter(stock -> tickersUpdated.contains(stock.getTicker())).toList();
        syncPersistenceService.partitionDataAndSaveWithLogTime(stocksUpdated, stockRepository, "saved " + stocksUpdated.size() + " stocks after generating high-lows 4w, 52w, all-time for the first import of the week");
    }

    public LocalDate findLastUpdate() {
        return cacheService.getStocksMap().values().stream()
                .max(Comparator.comparing(Stock::getLastUpdated))
                .map(Stock::getLastUpdated)
                .orElseThrow();
    }

    @Transactional
    public void updateStockDailyPricesFor(String ticker) {
        stockRepository.updateStockDailyPricesFor(ticker);
    }

    @Transactional
    public void updateStockHigherTimeframePricesFor(String ticker) {
        stockRepository.updateStockWeeklyPricesFor(ticker);
        stockRepository.updateStockMonthlyPricesFor(ticker);
        stockRepository.updateStockQuarterlyPricesFor(ticker);
        stockRepository.updateStockYearlyPricesFor(ticker);
    }

    @Transactional
    public void updateHighLowForPeriodPrices(String ticker) {
        stockRepository.updateHighLow4wPricesFor(ticker);
        stockRepository.updateHighLow52wPricesFor(ticker);
        stockRepository.updateHighestLowestPricesFor(ticker);
    }

    @Transactional
    public void renameTicker(String oldTicker, String newTicker) {
        log.info("Renamed ticker for Stock. Updated {} rows", tickerRenameRepository.updateStock(oldTicker, newTicker));
        log.info("Renamed ticker for DailyPrices. Updated {} rows", tickerRenameRepository.updateDailyPrices(oldTicker, newTicker));
        log.info("Renamed ticker for WeeklyPrices. Updated {} rows", tickerRenameRepository.updateWeeklyPrices(oldTicker, newTicker));
        log.info("Renamed ticker for MonthlyPrices. Updated {} rows", tickerRenameRepository.updateMonthlyPrices(oldTicker, newTicker));
        log.info("Renamed ticker for QuarterlyPrices. Updated {} rows", tickerRenameRepository.updateQuarterlyPrices(oldTicker, newTicker));
        log.info("Renamed ticker for YearlyPrices. Updated {} rows", tickerRenameRepository.updateYearlyPrices(oldTicker, newTicker));
        log.info("Renamed ticker for DailyPriceJSON. Updated {} rows", tickerRenameRepository.updateDailyPriceJson(oldTicker, newTicker));
        log.info("Renamed ticker for HighLow4w. Updated {} rows", tickerRenameRepository.updateHighLow4w(oldTicker, newTicker));
        log.info("Renamed ticker for HighLow52Week. Updated {} rows", tickerRenameRepository.updateHighLow52Week(oldTicker, newTicker));
        log.info("Renamed ticker for HighestLowestPrices. Updated {} rows", tickerRenameRepository.updateHighestLowestPrices(oldTicker, newTicker));
        log.info("Renamed ticker for FairValueGap. Updated {} rows", tickerRenameRepository.updateFairValueGap(oldTicker, newTicker));
        log.info("Renamed ticker for PriceGap. Updated {} rows", tickerRenameRepository.updatePriceGap(oldTicker, newTicker));
    }
}
