package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.model.stocks.enums.MarketState;
import stock.price.analytics.repository.stocks.StockRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final PriceService priceService;
    private final HighLowForPeriodService highLowForPeriodService;
    private final FairValueGapService fairValueGapService;
    private final StockRepository stockRepository;
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
    public void updateStocksHighLowsAndOHLCFrom() {
        List<DailyPrice> dailyPrices = cacheService.getCachedDailyPrices(MarketState.REGULAR);
        List<AbstractPrice> htfPrices = cacheService.pricesFor(StockTimeframe.WEEKLY);
        htfPrices.addAll(cacheService.pricesFor(StockTimeframe.MONTHLY));
        htfPrices.addAll(cacheService.pricesFor(StockTimeframe.QUARTERLY));
        htfPrices.addAll(cacheService.pricesFor(StockTimeframe.YEARLY));
        updateStocksHighLowsAndOHLCFrom(dailyPrices, htfPrices);
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
                .map(Stock::getLastUpdated)
                .max(LocalDate::compareTo)
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

    public void splitAdjustFor(String ticker, LocalDate stockSplitDate, double priceMultiplier) {
        priceService.adjustPricesFor(ticker, stockSplitDate, priceMultiplier);
        highLowForPeriodService.saveAllHistoricalHighLowPrices(List.of(ticker), stockSplitDate);
        fairValueGapService.updateFVGPricesForStockSplit(ticker, stockSplitDate, priceMultiplier);

        // stockSplitDate within the last_updated week
        if (stockSplitDate.isAfter(tradingDateNow().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))) {
            updateStockHigherTimeframePricesFor(ticker);
            updateHighLowForPeriodPrices(ticker);
            if (stockSplitDate.isEqual(tradingDateNow())) {
                updateStockDailyPricesFor(ticker);
            }
        }
    }
}
