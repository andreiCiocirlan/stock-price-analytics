package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HighLowPricesCache;
import stock.price.analytics.cache.StocksCache;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighestLowestPrices;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveNoLogging;
import static stock.price.analytics.util.PricesOHLCUtil.tickerFrom;
import static stock.price.analytics.util.TradingDateUtil.tradingDateImported;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StocksCache stocksCache;
    private final HighLowPricesCache highLowPricesCache;

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
        stockRepository.saveAll(stocks);
    }

    @Transactional
    public void saveStocks(String tickers, boolean xtbStock, boolean shortSell, double cfdMargin) {
        for (String ticker : tickers.split(",")) {
            stockRepository.save(new Stock(ticker, xtbStock, shortSell, cfdMargin));
        }
    }

    @Transactional
    public void updateStocksDate(List<DailyPriceOHLC> dailyImportedPrices) {
        logTime(() -> {
            LocalDate tradingDate = tradingDateImported(dailyImportedPrices);
            List<String> tickers = dailyImportedPrices.stream()
                    .filter(dailyPriceOHLC -> dailyPriceOHLC.getDate().equals(tradingDate)) // make sure only today prices are filtered
                    .map(DailyPriceOHLC::getTicker)
                    .toList();

            List<String> tickersCached = stocksCache.tickers();
            if (new HashSet<>(tickersCached).containsAll(tickers)) {
                List<Stock> stocksCached = stocksCache.stocksFor(tickers);
                stocksCached.forEach(s -> s.setLastUpdated(tradingDate));
                partitionDataAndSave(stocksCached, stockRepository);
            } else { // new stock imported that moment -> revert to regular sql update
                stockRepository.updateStocksLastUpdated(tradingDate, tickers);
            }
        }, "updated stocks date");
    }

    @Transactional
    public void updateStocksHighLow(LocalDate tradingDate) {
        logTime(() -> stockRepository.updateStocksHighLow(tradingDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))),
                "updated stocks high low 4w, 52w, all-time");
    }

    public void updateStocksHighLowFromHighLowCache(List<String> tickers) {
        Map<String, Stock> stocksMap = stocksCache.stocksMap();
        Map<String, HighLow4w> highLow4wMap = highLowPricesCache.highLowMapFor(HighLowPeriod.HIGH_LOW_4W);
        Map<String, HighLow52Week> highLow52wMap = highLowPricesCache.highLowMapFor(HighLowPeriod.HIGH_LOW_52W);
        Map<String, HighestLowestPrices> highestLowestMap = highLowPricesCache.highLowMapFor(HighLowPeriod.HIGH_LOW_ALL_TIME);
        for (String ticker : tickers) {
            Stock stock = stocksMap.get(ticker);
            highLow4wMap.get(ticker);
            stock.updateFrom(highLow4wMap.get(ticker), highLow52wMap.get(ticker), highestLowestMap.get(ticker));
        }
        partitionDataAndSaveNoLogging(stocksMap.values().stream().toList(), stockRepository);
    }

    public void initStocksCache() {
        stocksCache.addStocks(stockRepository.findByXtbStockTrueAndDelistedDateIsNull());
    }
}
