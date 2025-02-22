package stock.price.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.json.*;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.repository.prices.DailyPricesJSONRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.Files.readAllLines;
import static stock.price.analytics.util.Constants.MAX_TICKER_COUNT_PRINT;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveWithLogTime;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyPricesJSONService {

    private static final List<String> inconsistentHighs = new ArrayList<>();
    private static final List<String> inconsistentLows = new ArrayList<>();
    private final DailyPricesJSONCacheService dailyPricesJSONCacheService;
    private final DailyPricesService dailyPricesService;
    private final DailyPricesJSONRepository dailyPricesJSONRepository;

    public List<DailyPrice> dailyPricesFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            return dailyPricesFrom(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPricesJSON> dailyPricesJSONFrom(String jsonData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDate.class, new UnixTimestampToLocalDateDeserializer());
            objectMapper.registerModule(module);
            Response response = objectMapper.readValue(jsonData, Response.class);
            List<DailyPricesJSON> dailyPricesJSON = response.getQuoteResponse().getResult();

            return extractDailyJSONPricesAndSave(dailyPricesJSON, dailyPricesJSONCacheService.dailyPricesJSONCache());
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<DailyPricesJSON> extractDailyJSONPricesAndSave(List<DailyPricesJSON> dailyPricesJSON, List<DailyPricesJSON> recentJsonPrices) {
        List<String> sameDailyPrices = new ArrayList<>();
        List<DailyPricesJSON> dailyJSONPrices = new ArrayList<>();
        Map<String, DailyPricesJSON> recentJsonPricesById = recentJsonPrices.stream().collect(Collectors.toMap(DailyPricesJSON::getCompositeId, p -> p));
        LocalDate tradingDateNow = tradingDateNow();
        for (DailyPricesJSON dailyPriceJson : dailyPricesJSON) {
            String ticker = dailyPriceJson.getSymbol();
            LocalDate tradingDate = dailyPriceJson.getDate();
            if (!tradingDateNow.equals(tradingDate)) {
                if (tradingDate == null) {
                    log.warn("trading date missing for ticker {}", ticker);
                    continue;
                } else if (tradingDate.plusDays(5).isBefore(tradingDateNow)) {
                    // more than 5 days passed since last intraDay price
                    log.warn("Not extracting delisted stock daily prices for ticker {} and date {}", ticker, tradingDate);
                    continue;
                } else {
                    // less than 5 days passed since last intraDay price
                    log.warn("Extracting stock daily prices for ticker {} and date {}", ticker, tradingDate);
                }
            }
            compareAndAddToList(dailyPriceJson, recentJsonPricesById, dailyJSONPrices, sameDailyPrices, ticker);
        }

        logInconsistentHighLowImportedPrices();

        if (!sameDailyPrices.isEmpty()) {
            log.warn("same {} daily prices json as in DB", sameDailyPrices.size());
            if (sameDailyPrices.size() <= MAX_TICKER_COUNT_PRINT) {
                log.warn("{}", sameDailyPrices);
            }
        }
        List<DailyPricesJSON> dailyPricesJSONSInCache = dailyPricesJSONCacheService.addDailyPricesJSONInCacheAndReturn(dailyJSONPrices);
        if (!dailyPricesJSONSInCache.isEmpty()) {
            partitionDataAndSaveWithLogTime(dailyPricesJSONSInCache, dailyPricesJSONRepository, "saved " + dailyJSONPrices.size() + " daily json prices");
        }

        return dailyPricesJSONSInCache;
    }

    public List<DailyPricesJSON> dailyPricesJSONFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDate.class, new UnixTimestampToLocalDateDeserializer());
            objectMapper.registerModule(module);
            Response response = objectMapper.readValue(jsonData, Response.class);
            List<DailyPricesJSON> dailyPricesJSON = response.getQuoteResponse().getResult();

            LocalDate to = LocalDate.parse(fileName.split("_")[0], DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            LocalDate from = to.minusDays(10);

            return extractAllDailyPricesJSONFrom(dailyPricesJSON, dailyPricesJSONRepository.findByDateBetween(from, to));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPrice> extractDailyPricesFromJSON(String pricesJSON) {
        List<DailyPricesJSON> dailyPricesJSON = dailyPricesJSONFrom(pricesJSON);

        // Cache premarket prices if during pre-market hours
        List<DailyPrice> preMarketPrices = dailyPricesJSON.stream()
                .filter(dp -> dp.getPreMarketPrice() != 0d)
                .map(dp -> dp.convertToDailyPrice(true))
                .toList();

        if (!preMarketPrices.isEmpty()) {
            dailyPricesService.addPreMarketDailyPricesInCache(preMarketPrices);
        }
        return dailyPricesFrom(dailyPricesJSON);
    }

    public List<DailyPrice> dailyPricesFrom(List<DailyPricesJSON> dailyPricesJSON) {
        return dailyPricesJSON.stream()
                .map((dp -> dp.convertToDailyPrice(false)))
                .toList();
    }

    public List<DailyPrice> dailyPricesFrom(String jsonData) {
        return dailyPricesJSONFrom(jsonData).stream()
                .map((dp -> dp.convertToDailyPrice(false)))
                .toList();
    }

    public List<DailyPricesJSON> extractAllDailyPricesJSONFrom(List<DailyPricesJSON> dailyPricesJSON, List<DailyPricesJSON> recentJsonPrices) {
        List<String> sameDailyPrices = new ArrayList<>();
        List<DailyPricesJSON> dailyJSONPrices = new ArrayList<>();
        Map<String, DailyPricesJSON> recentJsonPricesById = recentJsonPrices.stream().collect(Collectors.toMap(DailyPricesJSON::getCompositeId, p -> p));

        for (DailyPricesJSON dailyPriceJson : dailyPricesJSON) {
            String ticker = dailyPriceJson.getSymbol();
            LocalDate tradingDate = dailyPriceJson.getDate();
            if (tradingDate == null) {
                log.warn("trading date missing from json file for ticker {}", ticker);
                continue;
            }
            compareAndAddToList(dailyPriceJson, recentJsonPricesById, dailyJSONPrices, sameDailyPrices, ticker);
        }

        if (!sameDailyPrices.isEmpty()) {
            log.warn("same {} daily prices json as in DB", sameDailyPrices.size());
            if (sameDailyPrices.size() <= MAX_TICKER_COUNT_PRINT) {
                log.warn("{}", sameDailyPrices);
            }
        }
        return dailyJSONPrices;
    }

    private void compareAndAddToList(DailyPricesJSON importedDailyPriceJSON, Map<String, DailyPricesJSON> recentJsonPricesById, List<DailyPricesJSON> dailyJSONPrices, List<String> sameDailyPrices, String ticker) {
        String key = importedDailyPriceJSON.getCompositeId();
        if (recentJsonPricesById.containsKey(key)) {
            DailyPricesJSON storedDailyPriceJSON = recentJsonPricesById.get(key);
            if (importedDailyPriceJSON.getRegularMarketDayHigh() < storedDailyPriceJSON.getRegularMarketDayHigh()) {
                inconsistentLows.add(importedDailyPriceJSON.getSymbol());
                importedDailyPriceJSON.setRegularMarketDayHigh(storedDailyPriceJSON.getRegularMarketDayHigh());
            }
            if (importedDailyPriceJSON.getRegularMarketDayLow() > storedDailyPriceJSON.getRegularMarketDayLow()) {
                inconsistentHighs.add(importedDailyPriceJSON.getSymbol());
                importedDailyPriceJSON.setRegularMarketDayLow(storedDailyPriceJSON.getRegularMarketDayLow());
            }
            if (importedDailyPriceJSON.getPreMarketPrice() != 0d || storedDailyPriceJSON.differentPrices(importedDailyPriceJSON)) { // compare OHLC, performance, or if pre-market price
                dailyJSONPrices.add(storedDailyPriceJSON.updateFrom(importedDailyPriceJSON));
            } else {
                sameDailyPrices.add(ticker);
            }
        } else {
            dailyJSONPrices.add(importedDailyPriceJSON);
        }
    }

    public void saveDailyPricesJSONFrom(String fileName) {
        String[] split = fileName.split("a");
        Set<DailyPricesJSON> dailyPricesJSON = new HashSet<>();
        Arrays.stream(split)
                .parallel().forEachOrdered(srcFile -> dailyPricesJSON.addAll(
                        logTimeAndReturn(() -> dailyPricesJSONFromFile(srcFile), "imported daily json prices")));
        Set<String> seenIds = new HashSet<>();
        List<DailyPricesJSON> dailyPricesJSON_toSave = dailyPricesJSON.stream()
                .filter(price -> seenIds.add(price.getCompositeId()))
                .toList();

        if (!dailyPricesJSON_toSave.isEmpty()) {
            partitionDataAndSaveWithLogTime(dailyPricesJSON_toSave, dailyPricesJSONRepository, "saved " + dailyPricesJSON_toSave.size() + " daily json prices");
        }
    }

    public void exportDailyPricesToJson(LocalDate date) {
        List<DailyPricesJSON> dailyPrices = dailyPricesJSONRepository.findByDate(date);
        QuoteResponse quoteResponse = new QuoteResponse();
        quoteResponse.setResult(dailyPrices);

        Response response = new Response();
        response.setQuoteResponse(quoteResponse);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateToUnixTimestampSerializer());
        objectMapper.registerModule(module);

        try {
            String path = "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/";
            objectMapper.writeValue(new File(path + date + ".json"), response);
            log.info("Data exported to JSON file successfully.");
        } catch (IOException e) {
            log.error("trading date missing from json file for ticker {}", e.getMessage());
        }
    }

    public void initDailyJSONPricesCache() {
        LocalDate tradingDateNow = tradingDateNow();
        // add last 7 trading days in  cache for good measure
        dailyPricesJSONCacheService.initDailyJSONPricesCache(dailyPricesJSONRepository.findByDateBetween(tradingDateNow.minusDays(7), tradingDateNow));
    }

    private void logInconsistentHighLowImportedPrices() {
        if (!inconsistentHighs.isEmpty()) {
            log.warn("Inconsistent imported highs for {}", inconsistentHighs);
        }
        if (!inconsistentLows.isEmpty()) {
            log.warn("Inconsistent imported lows for {}", inconsistentLows);
        }
        inconsistentHighs.clear();
        inconsistentLows.clear();
    }
}
