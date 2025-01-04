package stock.price.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.json.DailyPricesJSON;
import stock.price.analytics.model.prices.json.Response;
import stock.price.analytics.model.prices.json.UnixTimestampToLocalDateDeserializer;
import stock.price.analytics.repository.prices.DailyPricesJSONRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.Files.readAllLines;
import static stock.price.analytics.util.Constants.MAX_TICKER_COUNT_PRINT;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyPricesJSONService {

    private final DailyPricesJSONRepository dailyPricesJSONRepository;
    private final RefreshMaterializedViewsService refreshMaterializedViewsService;

    public List<DailyPricesJSON> findByDateBetween(LocalDate from, LocalDate to) {
        return dailyPricesJSONRepository.findByDateBetween(from, to);
    }

    public List<DailyPricesJSON> extractDailyJSONPricesAndSave(List<DailyPricesJSON> dailyPricesJSON) {
        List<DailyPricesJSON> recentJsonPrices = findByDateBetween(tradingDateNow().minusDays(7), tradingDateNow());
        Map<String, DailyPricesJSON> latestJsonPricesById = recentJsonPrices.stream().collect(Collectors.toMap(DailyPricesJSON::getCompositeId, p -> p));
        List<String> sameDailyPrices = new ArrayList<>();
        List<DailyPricesJSON> dailyJSONPrices = new ArrayList<>();
        for (DailyPricesJSON dailyPriceJson : dailyPricesJSON) {
            String ticker = dailyPriceJson.getSymbol();
            LocalDate tradingDate = dailyPriceJson.getDate();
            LocalDate tradingDateNow = tradingDateNow();
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
            String key = dailyPriceJson.getCompositeId();
            if (latestJsonPricesById.containsKey(key)) {
                DailyPricesJSON found = latestJsonPricesById.get(key);
                if (found.differentPrices(dailyPriceJson)) { // compare OHLC, performance
                    dailyJSONPrices.add(found.updateFrom(dailyPriceJson));
                } else {
                    sameDailyPrices.add(ticker);
                }
            } else {
                dailyJSONPrices.add(dailyPriceJson);
            }
        }

        if (!sameDailyPrices.isEmpty()) {
            log.warn("same {} daily prices as in DB", sameDailyPrices.size());
            if (sameDailyPrices.size() <= MAX_TICKER_COUNT_PRINT) {
                log.warn("{}", sameDailyPrices);
            }
        }
        dailyPricesJSONRepository.saveAll(dailyJSONPrices);

        return dailyJSONPrices;
    }

    public List<DailyPricesJSON> dailyPricesJSONFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            return extractAllDailyPricesJSONFrom(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPricesJSON> extractAllDailyPricesJSONFrom(String jsonData) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> sameDailyPrices = new ArrayList<>();
        List<DailyPricesJSON> dailyJSONPrices = new ArrayList<>();
        try {
            objectMapper.registerModule(new JavaTimeModule());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDate.class, new UnixTimestampToLocalDateDeserializer());
            objectMapper.registerModule(module);
            Response dailyPricesJSON = objectMapper.readValue(jsonData, Response.class);
            List<DailyPricesJSON> recentJsonPrices = findByDateBetween(tradingDateNow().minusDays(700), tradingDateNow());
            Map<String, DailyPricesJSON> recentJsonPricesById = recentJsonPrices.stream().collect(Collectors.toMap(DailyPricesJSON::getCompositeId, p -> p));

            for (DailyPricesJSON dailyPriceJson : dailyPricesJSON.getQuoteResponse().getResult()) {
                String ticker = dailyPriceJson.getSymbol();
                LocalDate tradingDate = dailyPriceJson.getDate();
                if (tradingDate == null) {
                    log.warn("trading date missing from json file for ticker {}", ticker);
                    continue;
                }
                String key = dailyPriceJson.getCompositeId();
                if (recentJsonPricesById.containsKey(key)) {
                    DailyPricesJSON found = recentJsonPricesById.get(key);
                    if (found.differentPrices(dailyPriceJson)) { // compare OHLC, performance
                        dailyJSONPrices.add(found.updateFrom(dailyPriceJson));
                    } else {
                        sameDailyPrices.add(ticker);
                    }
                } else {
                    dailyJSONPrices.add(dailyPriceJson);
                }
            }
        } catch (JsonProcessingException ex) {
            log.error("Something went wrong processing JSON data {}", ex.getMessage());
            throw new RuntimeException(ex);
        }

        if (!sameDailyPrices.isEmpty()) {
            log.warn("same {} daily prices as in DB", sameDailyPrices.size());
            if (sameDailyPrices.size() <= MAX_TICKER_COUNT_PRINT) {
                log.warn("{}", sameDailyPrices);
            }
        }
        return dailyJSONPrices;
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
            partitionDataAndSave(dailyPricesJSON_toSave, dailyPricesJSONRepository);
            refreshMaterializedViewsService.refreshDailyJSONPrices();
        }
    }
}
