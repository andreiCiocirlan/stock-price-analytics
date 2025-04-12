package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.client.YahooQuotesClient;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.repository.prices.ohlc.DailyPriceRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static stock.price.analytics.model.prices.enums.IntradayPriceSpike.intradaySpikes;
import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X_3X;
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
    private final AsyncPersistenceService asyncPersistenceService;
    private final DailyPriceJSONService dailyPriceJSONService;
    private final PriceMilestoneService priceMilestoneService;
    private final DailyPriceRepository dailyPriceRepository;

    public List<DailyPrice> yahooQuotesFromFile(String fileName) {
        List<DailyPrice> dailyPrices = dailyPriceJSONService.dailyPricesFromFile(fileName);
        return cacheService.cacheAndReturnDailyPrices(dailyPrices);
    }

    @Transactional
    public List<DailyPrice> yahooQuotesImport() {
        List<String> cachedTickers = cacheService.getCachedTickers();
        List<String> tickersNotImported = cacheService.getCachedTickers();
        List<String> pricesJSONs = logTimeAndReturn(() -> yahooQuotesClient.quotePricesFor(cachedTickers), "Yahoo API call and JSON result");
        String pricesJSON = mergedPricesJSONs(pricesJSONs);
        List<DailyPrice> dailyPricesExtractedFromJSON = dailyPriceJSONService.extractDailyPricesFromJSON(pricesJSON);
        List<DailyPrice> dailyImportedPrices = cacheService.cacheAndReturnDailyPrices(dailyPricesExtractedFromJSON);

        // keep track of which tickers were imported
        tickersNotImported.removeAll(dailyImportedPrices.stream().map(DailyPrice::getTicker).toList());

        if (!dailyImportedPrices.isEmpty()) {
            String fileName = tradingDateImported(dailyPricesExtractedFromJSON).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".json";
            String path = "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/" + fileName;
            writeToFile(path, pricesJSON);
            asyncPersistenceService.partitionDataAndSaveWithLogTime(dailyImportedPrices, dailyPriceRepository, "saved " + dailyImportedPrices.size() + " daily prices");
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
        priceMilestoneService.findTickersForMilestones(intradaySpikes(), CFD_MARGINS_5X_4X_3X)
                .forEach(cacheService::cachePriceMilestoneTickers);

        return dailyImportedPrices;
    }



}
