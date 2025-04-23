package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.client.YahooQuotesClient;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static stock.price.analytics.util.Constants.MAX_TICKER_COUNT_PRINT;
import static stock.price.analytics.util.FileUtil.writeToFile;
import static stock.price.analytics.util.JsonUtil.mergedPricesJSONs;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.TradingDateUtil.tradingDateImported;

@Slf4j
@Service
@RequiredArgsConstructor
public class YahooQuoteService {

    private final YahooQuotesClient yahooQuotesClient;
    private final CacheService cacheService;
    private final DailyPriceJSONService dailyPriceJSONService;

    public List<DailyPrice> yahooQuotesFromFile(String fileName) {
        List<DailyPrice> dailyPrices = dailyPriceJSONService.dailyPricesFromFile(fileName);
        cacheService.updateIntradayPriceSpikesCache(dailyPrices);
        return dailyPrices;
    }

    @Transactional
    public List<DailyPrice> yahooQuotesImport() {
        List<String> cachedTickers = cacheService.getCachedTickers();
        List<String> tickersNotImported = cacheService.getCachedTickers();
        List<String> pricesJSONs = logTimeAndReturn(() -> yahooQuotesClient.quotePricesFor(cachedTickers), "Yahoo API call and JSON result");
        String pricesJSON = mergedPricesJSONs(pricesJSONs);
        List<DailyPrice> dailyImportedPrices = dailyPriceJSONService.extractDailyPricesFromJSON(pricesJSON);

        // keep track of which tickers were imported
        tickersNotImported.removeAll(dailyImportedPrices.stream().map(DailyPrice::getTicker).toList());

        if (!dailyImportedPrices.isEmpty()) {
            String fileName = tradingDateImported(dailyImportedPrices).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".json";
            String path = "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/" + fileName;
            writeToFile(path, pricesJSON);
        }

        if (!tickersNotImported.isEmpty()) {
            log.info("Did not import {} tickers", tickersNotImported.size());
            if (tickersNotImported.size() <= MAX_TICKER_COUNT_PRINT) {
                log.info("{}", tickersNotImported);
            }
        } else {
            log.info("Imported {} tickers", dailyImportedPrices.size());
        }

        // cache Intraday Spike tickers (compares closing prices from dailyPrices and stocks cache, which was not updated yet)
        cacheService.updateIntradayPriceSpikesCache(dailyImportedPrices);

        return dailyImportedPrices;
    }


}
