package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.client.yahoo.YahooQuoteClient;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.DailyPricesRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.enums.IntradayPriceSpike.intradaySpikes;
import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X_3X;
import static stock.price.analytics.util.Constants.MAX_TICKER_COUNT_PRINT;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveWithLogTime;
import static stock.price.analytics.util.TradingDateUtil.tradingDateImported;

@Slf4j
@Service
@RequiredArgsConstructor
public class YahooQuoteService {

    private final YahooQuoteClient yahooQuoteClient;
    private final CacheService cacheService;
    private final DailyPricesJSONService dailyPricesJSONService;
    private final PriceMilestoneService priceMilestoneService;
    private final DailyPricesRepository dailyPricesRepository;

    public List<DailyPrice> yahooQuotesFromFile(String fileName) {
        List<DailyPrice> dailyPrices = dailyPricesJSONService.dailyPricesFromFile(fileName);
        return cacheService.cacheAndReturnDailyPrices(dailyPrices);
    }

    @Transactional
    public List<DailyPrice> yahooQuotesImport() {
        int maxTickersPerRequest = 100;
        List<DailyPrice> dailyImportedPrices = new ArrayList<>();
        List<Stock> cachedStocks = cacheService.getCachedStocks();
        List<String> tickersNotImported = new ArrayList<>(cachedStocks.stream().map(Stock::getTicker).toList());

        int start = 0;
        int end = Math.min(maxTickersPerRequest, cachedStocks.size());
        int fileCounter = 1;
        while (start < cachedStocks.size()) {
            List<Stock> partition = cachedStocks.subList(start, end);
            String tickers = partition.stream().map(Stock::getTicker).collect(Collectors.joining(","));
            String pricesJSON = logTimeAndReturn(() -> yahooQuoteClient.quotePricesJSON(tickers), "Yahoo API call and JSON result");

            List<DailyPrice> dailyPricesExtractedFromJSON = dailyPricesJSONService.extractDailyPricesFromJSON(pricesJSON);
            dailyImportedPrices.addAll(cacheService.cacheAndReturnDailyPrices(dailyPricesExtractedFromJSON));

            // keep track of which tickers were imported
            tickersNotImported.removeAll(dailyImportedPrices.stream().map(DailyPrice::getTicker).toList());

            if (!dailyImportedPrices.isEmpty()) {
                String fileName = tradingDateImported(dailyPricesExtractedFromJSON).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "_" + fileCounter + ".json";
                String path = "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/" + fileName;
                writeToFile(path, pricesJSON);
            }

            start = end;
            end = Math.min(start + maxTickersPerRequest, cachedStocks.size());
            fileCounter++;
        }

        if (!dailyImportedPrices.isEmpty()) {
            partitionDataAndSaveWithLogTime(dailyImportedPrices, dailyPricesRepository, "saved " + dailyImportedPrices.size() + " daily prices");
        }

        if (!tickersNotImported.isEmpty()) {
            log.warn("Did not import {} tickers", tickersNotImported.size());
            if (tickersNotImported.size() <= MAX_TICKER_COUNT_PRINT) {
                log.warn("{}", tickersNotImported);
            }
        } else {
            log.warn("Imported {} tickers", dailyImportedPrices.size());
        }

        // cache Intraday Spike tickers (compares closing prices from dailyPrices and stocks cache, which was not updated yet)
        priceMilestoneService.findTickersForMilestones(intradaySpikes(), CFD_MARGINS_5X_4X_3X)
                .forEach(cacheService::cachePriceMilestoneTickers);

        return dailyImportedPrices;
    }

    private void writeToFile(String filePath, String jsonData) {
        try {
            File jsonFile = new File(filePath);

            try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
                outputStream.write(jsonData.getBytes(StandardCharsets.UTF_8));
            }
            log.info("saved daily prices file {}", jsonFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error writing to file: {}", filePath, e);
        }
    }


}
