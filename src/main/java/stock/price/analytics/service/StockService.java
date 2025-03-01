package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HighLowPricesCacheService;
import stock.price.analytics.cache.StocksCache;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.util.Constants;
import stock.price.analytics.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveWithLogTime;
import static stock.price.analytics.util.PricesUtil.tickerFrom;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StocksCache stocksCache;
    private final HighLowPricesCacheService highLowPricesCacheService;

    @Transactional
    public void saveStocks() throws IOException {
        List<String> tickersXTB = FileUtils.readTickersXTB().stream().map(s -> s.concat(".csv")).toList();
        List<Stock> stocks = new ArrayList<>();
        List<String> existingStocks = stockRepository.findAll().stream().map(Stock::getTicker).toList();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .filter(srcFile -> !existingStocks.contains(tickerFrom(srcFile)))
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        String fileName = srcFile.getFileName().toString();
                        String ticker = fileName.substring(0, fileName.length() - 4);
                        boolean xtbStock = tickersXTB.contains(fileName);
                        stocks.add(new Stock(ticker, LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)), xtbStock));
                    });
        }
        boolean removed = stocks.removeAll(stockRepository.findAll());
        log.info("remaining stocks {}", stocks);
        log.info("removed {}", removed);
        partitionDataAndSave(stocks, stockRepository);
    }

    @Transactional
    public void saveStocks(String tickers, boolean xtbStock, boolean shortSell, double cfdMargin) {
        for (String ticker : tickers.split(",")) {
            stockRepository.save(new Stock(ticker, xtbStock, shortSell, cfdMargin));
        }
    }

    @Transactional
    public void updateStocksHighLow(LocalDate tradingDate) {
        logTime(() -> stockRepository.updateStocksHighLow(tradingDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))),
                "updated stocks high low 4w, 52w, all-time");
    }

    private void updateStocksFromOHLCPrices(List<DailyPrice> dailyPrices, List<AbstractPrice> htfPrices, Set<Stock> stocksUpdated) {
        Map<String, Stock> stocksMap = stocksCache.getStocksMap();
        // update from daily prices
        for (DailyPrice dailyPrice : dailyPrices) {
            String ticker = dailyPrice.getTicker();
            Stock stock = stocksMap.getOrDefault(ticker, new Stock(ticker, dailyPrice.getDate(), true));
            if (!stock.getLastUpdated().isAfter(dailyPrice.getDate())) { // don't update for imports from the past
                stock.updateFromDailyPrice(dailyPrice);
                stocksUpdated.add(stock);
            }
        }

        // update from higher timeframe prices
        for (AbstractPrice wmyPrice : htfPrices) {
            String ticker = wmyPrice.getTicker();
            Stock stock = stocksMap.getOrDefault(ticker, new Stock(ticker, wmyPrice.getStartDate(), true));
            switch (wmyPrice.getTimeframe()) {
                case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
                case WEEKLY -> stock.updateFromWeeklyPrice((WeeklyPrice) wmyPrice);
                case MONTHLY -> stock.updateFromMonthlyPrice((MonthlyPrice) wmyPrice);
                case QUARTERLY -> stock.updateFromQuarterlyPrice((QuarterlyPrice) wmyPrice);
                case YEARLY -> stock.updateFromYearlyPrice((YearlyPrice) wmyPrice);
            }
            stocksUpdated.add(stock);
        }
    }

    private void updateStocksFromHighLowCaches(Set<Stock> stocksUpdated) {
        Map<String, Stock> stocksMap = stocksCache.getStocksMap();

        for (HighLowPeriod period : HighLowPeriod.values()) {
            List<? extends HighLowForPeriod> cache = highLowPricesCacheService.cacheForHighLowPeriod(period);
            for (HighLowForPeriod hl : cache) {
                Stock stock = stocksMap.get(hl.getTicker());
                if (stock != null) { // Check if stock exists
                    stock.updateFrom(hl);
                    stocksUpdated.add(stock);
                }
            }
        }
    }

    public void updateStocksHighLowsAndOHLCFrom(List<DailyPrice> dailyPrices, List<AbstractPrice> htfPrices) {
        Set<Stock> stocksUpdated = new HashSet<>();
        updateStocksFromOHLCPrices(dailyPrices, htfPrices, stocksUpdated);
        updateStocksFromHighLowCaches(stocksUpdated);

        List<Stock> stocks = new ArrayList<>(stocksUpdated);
        partitionDataAndSaveWithLogTime(stocks, stockRepository, "saved stocks " + stocks.size() + " after OHLC higher-timeframe and high-lows 4w, 52w, all-time updates");
        stocksCache.addStocks(stocks);
    }

    public void updateHighLowForPeriodFromHLCachesAndAdjustWeekend() {
        Set<Stock> stocksUpdated = new HashSet<>();
        updateStocksFromHighLowCaches(stocksUpdated);

        for (Stock stock : stocksUpdated) {
            LocalDate lastUpdated = stock.getLastUpdated();
            if (lastUpdated.getDayOfWeek().equals(DayOfWeek.FRIDAY)) { // change last_updated to monday
                stock.setLastUpdated(lastUpdated.plusDays(3));
            }
        }
        List<Stock> stocks = new ArrayList<>(stocksUpdated);
        partitionDataAndSaveWithLogTime(stocks, stockRepository, "saved stocks after generating high-lows 4w, 52w, all-time for the first import of the week");
        stocksCache.addStocks(stocks);
    }

    public Map<String, Stock> stocksCacheMap() {
        return stocksCache.getStocksMap();
    }

    public void initStocksCache() {
        stocksCache.addStocks(stockRepository.findByXtbStockTrueAndDelistedDateIsNull());
        findAndDelistStocksFromCache();
    }

    private void findAndDelistStocksFromCache() {
        List<Stock> stocksDelisted = new ArrayList<>();
        for (Stock stock : stocksCache.getStocksMap().values()) {
            if (stock.getLastUpdated().isBefore(LocalDate.now().minusDays(5))) {
                log.warn("DELISTED stock {}", stock.getTicker());
                stock.setDelistedDate(stock.getLastUpdated());
                stocksDelisted.add(stock);
            }
        }
        if (!stocksDelisted.isEmpty()) {
            partitionDataAndSave(stocksDelisted, stockRepository);
        }
    }

    public LocalDate findLastUpdate() {
        return stocksCache.getStocksMap().values().stream()
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

    public List<Stock> getCachedStocks() {
        return stocksCache.getStocksMap().values().stream().toList();
    }
}
