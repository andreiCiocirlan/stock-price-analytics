package stock.price.analytics.client.yahoo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.json.DailyPricesJSON;
import stock.price.analytics.model.prices.json.Response;
import stock.price.analytics.model.prices.json.UnixTimestampToLocalDateDeserializer;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.service.DailyPricesJSONService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.readAllLines;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooFinanceClient {

    public final DailyPricesJSONService dailyPricesJSONService;

    public List<DailyPriceOHLC> dailyPricesFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            return extractDailyPricesFromJSON(jsonData, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPriceOHLC> extractDailyPricesFromJSON(String jsonData, boolean preMarketPrices) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<DailyPricesJSON> dailyPricesJSONList = new ArrayList<>();
        try {
            objectMapper.registerModule(new JavaTimeModule());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDate.class, new UnixTimestampToLocalDateDeserializer());
            objectMapper.registerModule(module);
            Response response = objectMapper.readValue(jsonData, Response.class);
            List<DailyPricesJSON> dailyPricesJSON = response.getQuoteResponse().getResult();

            dailyPricesJSONList.addAll(dailyPricesJSONService.extractDailyJSONPricesAndSave(dailyPricesJSON));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }

        return dailyPricesFrom(dailyPricesJSONList, preMarketPrices);
    }

    public List<DailyPriceOHLC> dailyPricesFrom(List<DailyPricesJSON> dailyPricesJSON, boolean preMarketPrices) {
        List<DailyPriceOHLC> dailyPrices = new ArrayList<>();
        for (DailyPricesJSON dailyPriceJson : dailyPricesJSON) {
            dailyPrices.add(dailyPriceJson.convertToDailyPrice(preMarketPrices));
        }
        return dailyPrices;
    }

}
