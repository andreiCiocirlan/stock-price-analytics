package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HighLowPricesCache;
import stock.price.analytics.cache.StocksCache;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PricesOHLCUtil.tickerFrom;

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
    public void updateStocksHighLow(LocalDate tradingDate) {
        logTime(() -> stockRepository.updateStocksHighLow(tradingDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))),
                "updated stocks high low 4w, 52w, all-time");
    }

    public void updateStocksOHLCFrom(List<DailyPriceOHLC> dailyImportedPrices) {
        Map<String, Stock> stocksMap = stocksCache.stocksMap();
        List<Stock> stocksUpdated = new ArrayList<>();
        for (DailyPriceOHLC dailyImportedPrice : dailyImportedPrices) {
            String ticker = dailyImportedPrice.getTicker();
            Stock stock = stocksMap.containsKey(ticker) ? stocksMap.get(ticker) : new Stock(ticker, LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)), true);
            stock.updateFromDailyPrice(dailyImportedPrice);
            stocksUpdated.add(stock);
        }
        partitionDataAndSave(stocksUpdated, stockRepository);
        stocksCache.addStocks(stocksUpdated);
    }

    public void updateStocksHighLowFromHighLowCache() {
        Map<String, Stock> stocksMap = stocksCache.stocksMap();
        List<HighLowForPeriod> newHighLowPrices = highLowPricesCache.getNewHighLowPrices();
        Set<Stock> updatedStocks = newHighLowPrices.stream()
                .map(HighLowForPeriod::getTicker)
                .filter(stocksMap::containsKey)
                .map(stocksMap::get)
                .collect(Collectors.toSet());
        if (!updatedStocks.isEmpty()) {
            for (HighLowForPeriod newHighLowPrice : newHighLowPrices) {
                String ticker = newHighLowPrice.getTicker();
                Stock stock = stocksMap.get(ticker);
                stock.updateFrom(newHighLowPrice);
            }
            List<Stock> stocks = updatedStocks.stream().toList();
            partitionDataAndSave(stocks, stockRepository);
            stocksCache.addStocks(stocks);
            highLowPricesCache.clearNewHighLowPrices(); // clear new high low prices for next import
        }
    }

    public void initStocksCache() {
        stocksCache.addStocks(stockRepository.findByXtbStockTrueAndDelistedDateIsNull());
    }
}
