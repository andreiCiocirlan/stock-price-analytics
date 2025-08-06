package stock.price.analytics.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.json.*;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.repository.json.DailyPriceJSONRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.Files.readAllLines;
import static stock.price.analytics.util.Constants.MAX_TICKER_COUNT_PRINT;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyPriceJSONService {

    private static final List<String> inconsistentHighs = new ArrayList<>();
    private static final List<String> inconsistentLows = new ArrayList<>();
    private final CacheService cacheService;
    private final AsyncPersistenceService asyncPersistenceService;
    private final DailyPriceJSONRepository dailyPriceJSONRepository;

    public List<DailyPrice> dailyPricesFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            return dailyPricesFrom(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPriceJSON> dailyPriceJSONsFrom(String jsonData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDate.class, new UnixTimestampToLocalDateDeserializer());
            objectMapper.registerModule(module);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            Response response = objectMapper.readValue(jsonData, Response.class);
            List<DailyPriceJSON> dailyPriceJSONs = response.getQuoteResponse().getResult();

            return extractDailyJSONPricesAndSave(dailyPriceJSONs, cacheService.dailyPriceJsonCache());
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Transactional
    public List<DailyPriceJSON> extractDailyJSONPricesAndSave(List<DailyPriceJSON> dailyPriceJSONs, List<DailyPriceJSON> recentJsonPrices) {
        List<String> sameDailyPrices = new ArrayList<>();
        List<DailyPriceJSON> dailyJSONPrices = new ArrayList<>();
        List<DailyPrice> preMarketPrices = new ArrayList<>();
        Map<String, DailyPriceJSON> recentJsonPricesById = recentJsonPrices.stream().collect(Collectors.toMap(DailyPriceJSON::getCompositeId, Function.identity()));
        LocalDate tradingDateNow = tradingDateNow();
        for (DailyPriceJSON dailyPriceJson : dailyPriceJSONs) {
            String ticker = dailyPriceJson.getSymbol();
            LocalDate tradingDate = dailyPriceJson.getDate();
            if (!tradingDateNow.equals(tradingDate)) {
                if (tradingDate == null) {
                    log.warn("trading date missing for ticker {}", ticker);
                    continue;
                } else if (tradingDate.plusDays(5).isBefore(tradingDateNow)) {
                    // more than 5 days passed since last intraDay price
                    log.info("Not extracting delisted stock daily prices for ticker {} and date {}", ticker, tradingDate);
                    continue;
                } else {
                    // less than 5 days passed since last intraDay price
                    log.info("Extracting stock daily prices for ticker {} and date {}", ticker, tradingDate);
                }
            }
            compareAndAddToList(dailyPriceJson, recentJsonPricesById, dailyJSONPrices, preMarketPrices, sameDailyPrices, ticker);
        }

        if (!preMarketPrices.isEmpty()) {
            cacheService.addPreMarketDailyPrices(preMarketPrices);
        }

        logInconsistentHighLowImportedPrices();

        if (!sameDailyPrices.isEmpty()) {
            log.info("same {} daily prices json as in DB", sameDailyPrices.size());
            if (sameDailyPrices.size() <= MAX_TICKER_COUNT_PRINT) {
                log.info("{}", sameDailyPrices);
            }
        }
        List<DailyPriceJSON> dailyPriceJsonCache = cacheService.addDailyPricesJSONAndReturn(dailyJSONPrices);
        if (!dailyPriceJsonCache.isEmpty()) {
            asyncPersistenceService.partitionDataAndSaveWithLogTime(dailyPriceJsonCache, dailyPriceJSONRepository, "saved " + dailyPriceJsonCache.size() + " daily json prices");
        }

        return dailyPriceJsonCache;
    }

    public List<DailyPriceJSON> dailyPriceJSONsFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDate.class, new UnixTimestampToLocalDateDeserializer());
            objectMapper.registerModule(module);
            Response response = objectMapper.readValue(jsonData, Response.class);
            List<DailyPriceJSON> dailyPriceJSONs = response.getQuoteResponse().getResult();

            LocalDate to = LocalDate.parse(fileName.split("_")[0], DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            LocalDate from = to.minusDays(10);

            return extractAllDailyPriceJSONsFrom(dailyPriceJSONs, dailyPriceJSONRepository.findByDateBetween(from, to));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPrice> extractDailyPricesFromJSON(String pricesJSON) {
        List<DailyPriceJSON> dailyPriceJSONs = dailyPriceJSONsFrom(pricesJSON);

        return dailyPricesFrom(dailyPriceJSONs);
    }

    public List<DailyPrice> dailyPricesFrom(List<DailyPriceJSON> dailyPriceJSONs) {
        return dailyPriceJSONs.stream()
                .map((dp -> dp.convertToDailyPrice(false)))
                .toList();
    }

    public List<DailyPrice> dailyPricesFrom(String jsonData) {
        return dailyPriceJSONsFrom(jsonData).stream()
                .map((dp -> dp.convertToDailyPrice(false)))
                .toList();
    }

    public List<DailyPriceJSON> extractAllDailyPriceJSONsFrom(List<DailyPriceJSON> dailyPriceJSONs, List<DailyPriceJSON> recentJsonPrices) {
        List<String> sameDailyPrices = new ArrayList<>();
        List<DailyPriceJSON> dailyJSONPrices = new ArrayList<>();
        List<DailyPrice> preMarketPrices = new ArrayList<>();
        Map<String, DailyPriceJSON> recentJsonPricesById = recentJsonPrices.stream().collect(Collectors.toMap(DailyPriceJSON::getCompositeId, Function.identity()));

        for (DailyPriceJSON dailyPriceJson : dailyPriceJSONs) {
            String ticker = dailyPriceJson.getSymbol();
            LocalDate tradingDate = dailyPriceJson.getDate();
            if (tradingDate == null) {
                log.warn("trading date missing from json file for ticker {}", ticker);
                continue;
            }
            compareAndAddToList(dailyPriceJson, recentJsonPricesById, dailyJSONPrices, preMarketPrices, sameDailyPrices, ticker);
        }
        if (!preMarketPrices.isEmpty()) {
            cacheService.addPreMarketDailyPrices(preMarketPrices);
        }

        if (!sameDailyPrices.isEmpty()) {
            log.info("same {} daily prices json as in DB", sameDailyPrices.size());
            if (sameDailyPrices.size() <= MAX_TICKER_COUNT_PRINT) {
                log.info("{}", sameDailyPrices);
            }
        }

        logInconsistentHighLowImportedPrices();

        return dailyJSONPrices;
    }

    private boolean shouldUpdatePrice(DailyPriceJSON imported, DailyPriceJSON stored) {
        return imported.getPreMarketPrice() != 0d || stored.differentPrices(imported);
    }

    private void updateAndAddPrice(DailyPriceJSON stored, DailyPriceJSON imported, List<DailyPriceJSON> dailyJSONPrices, List<DailyPrice> preMarketPrices) {
        Pair<Double, Double> highLowPrices = getHighLowImportedPrices(imported, stored);
        DailyPriceJSON updatedPrice = stored.updateFrom(imported);
        updatedPrice.setRegularMarketDayHigh(highLowPrices.getLeft());
        updatedPrice.setRegularMarketDayLow(highLowPrices.getRight());

        if (imported.getPreMarketPrice() != 0d) {
            preMarketPrices.add(updatedPrice.convertToDailyPrice(true));
        } else {
            dailyJSONPrices.add(updatedPrice);
        }
    }

    private void compareAndAddToList(DailyPriceJSON importedDailyPriceJSON,
                                     Map<String, DailyPriceJSON> recentJsonPricesById,
                                     List<DailyPriceJSON> dailyJSONPrices,
                                     List<DailyPrice> preMarketPrices,
                                     List<String> sameDailyPrices,
                                     String ticker) {

        String key = importedDailyPriceJSON.getCompositeId();

        if (!recentJsonPricesById.containsKey(key)) {
            dailyJSONPrices.add(importedDailyPriceJSON);
            return;
        }

        DailyPriceJSON storedDailyPriceJSON = recentJsonPricesById.get(key);

        if (!shouldUpdatePrice(importedDailyPriceJSON, storedDailyPriceJSON)) {
            sameDailyPrices.add(ticker);
            return;
        }

        updateAndAddPrice(storedDailyPriceJSON, importedDailyPriceJSON, dailyJSONPrices, preMarketPrices);
    }


    // utility method to find inconsistencies between imported high-low prices and already stored prices
    private Pair<Double, Double> getHighLowImportedPrices(DailyPriceJSON importedDailyPriceJSON, DailyPriceJSON storedDailyPriceJSON) {
        double high = importedDailyPriceJSON.getRegularMarketDayHigh();
        double low = importedDailyPriceJSON.getRegularMarketDayLow();
        // abnormal -> imported high price cannot be smaller than already stored high price
        if (importedDailyPriceJSON.getRegularMarketDayHigh() < storedDailyPriceJSON.getRegularMarketDayHigh()) {
            inconsistentLows.add(importedDailyPriceJSON.getSymbol());
            high = storedDailyPriceJSON.getRegularMarketDayHigh();
        }
        // abnormal -> imported low price cannot be greater than already stored low price
        if (importedDailyPriceJSON.getRegularMarketDayLow() > storedDailyPriceJSON.getRegularMarketDayLow()) {
            inconsistentHighs.add(importedDailyPriceJSON.getSymbol());
            low = storedDailyPriceJSON.getRegularMarketDayLow();
        }
        return new MutablePair<>(high, low);
    }

    @Transactional
    public void saveDailyPriceJSONsFrom(String fileName) {
        String[] split = fileName.split("a");
        Set<DailyPriceJSON> dailyPriceJSONs = new HashSet<>();
        Arrays.stream(split)
                .parallel().forEachOrdered(srcFile -> dailyPriceJSONs.addAll(
                        logTimeAndReturn(() -> dailyPriceJSONsFromFile(srcFile), "imported daily json prices")));
        Set<String> seenIds = new HashSet<>();
        List<DailyPriceJSON> dailyPriceJSONs_toSave = dailyPriceJSONs.stream()
                .filter(price -> seenIds.add(price.getCompositeId()))
                .toList();

        if (!dailyPriceJSONs_toSave.isEmpty()) {
            asyncPersistenceService.partitionDataAndSaveWithLogTime(dailyPriceJSONs_toSave, dailyPriceJSONRepository, "saved " + dailyPriceJSONs_toSave.size() + " daily json prices");
        }
    }

    public void exportDailyPricesToJson(LocalDate date) {
        List<DailyPriceJSON> dailyPrices = dailyPriceJSONRepository.findByDate(date);
        QuoteResponse quoteResponse = new QuoteResponse();
        quoteResponse.setResult(dailyPrices);

        Response response = new Response(quoteResponse);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateToUnixTimestampSerializer());
        objectMapper.registerModule(module);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String path = "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/";
            objectMapper.writeValue(new File(path + date + ".json"), response);
            log.info("Data exported to JSON file successfully.");
        } catch (IOException e) {
            log.error("trading date missing from json file for ticker {}", e.getMessage());
        }
    }

    private void logInconsistentHighLowImportedPrices() {
        if (!inconsistentHighs.isEmpty()) {
            log.info("Inconsistent DAILY PRICES JSON imported highs for {}", inconsistentHighs);
        }
        if (!inconsistentLows.isEmpty()) {
            log.info("Inconsistent DAILY PRICES JSON imported lows for {}", inconsistentLows);
        }
        inconsistentHighs.clear();
        inconsistentLows.clear();
    }
}
