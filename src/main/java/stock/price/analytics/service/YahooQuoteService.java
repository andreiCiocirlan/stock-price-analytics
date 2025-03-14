package stock.price.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        List<Stock> cachedStocks = cacheService.getCachedStocks();
        List<String> tickersNotImported = new ArrayList<>(cachedStocks.stream().map(Stock::getTicker).toList());
        List<String> pricesJSONs = logTimeAndReturn(() -> yahooQuoteClient.quotePricesFor(cachedStocks.stream().map(Stock::getTicker).toList()), "Yahoo API call and JSON result");
        String pricesJSON = mergedPricesJSONs(pricesJSONs);
        List<DailyPrice> dailyPricesExtractedFromJSON = dailyPricesJSONService.extractDailyPricesFromJSON(pricesJSON);
        List<DailyPrice> dailyImportedPrices = cacheService.cacheAndReturnDailyPrices(dailyPricesExtractedFromJSON);

        // keep track of which tickers were imported
        tickersNotImported.removeAll(dailyImportedPrices.stream().map(DailyPrice::getTicker).toList());

        if (!dailyImportedPrices.isEmpty()) {
            String fileName = tradingDateImported(dailyPricesExtractedFromJSON).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".json";
            String path = "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/" + fileName;
            writeToFile(path, pricesJSON);
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

    private String mergedPricesJSONs(List<String> pricesJSONs) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode mergedQuoteResponse = objectMapper.createObjectNode();
            ArrayNode mergedResults = objectMapper.createArrayNode();
            for (String json : pricesJSONs) {
                JsonNode rootNode = objectMapper.readTree(json);
                JsonNode results = rootNode.path("quoteResponse").path("result");

                if (results.isArray()) {
                    results.forEach(mergedResults::add);
                }
            }
            mergedQuoteResponse.set("result", mergedResults);
            ObjectNode finalResponse = objectMapper.createObjectNode();
            finalResponse.set("quoteResponse", mergedQuoteResponse);

            return objectMapper.writeValueAsString(finalResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
