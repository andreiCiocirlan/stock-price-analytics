package stock.price.analytics.client.yahoo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.json.DailyPricesJSON;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.service.DailyPricesJSONService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.readAllLines;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooFinanceClient {

    public final DailyPricesJSONService dailyPricesJSONService;

    public List<DailyPrice> dailyPricesFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            return extractDailyPricesFromJSON(jsonData, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPrice> extractDailyPricesFromJSON(String jsonData, boolean preMarketPrices) {
        return dailyPricesFrom(dailyPricesJSONService.dailyPricesJSONFrom(jsonData), preMarketPrices);
    }

    public List<DailyPrice> dailyPricesFrom(List<DailyPricesJSON> dailyPricesJSON, boolean preMarketPrices) {
        List<DailyPrice> dailyPrices = new ArrayList<>();
        for (DailyPricesJSON dailyPriceJson : dailyPricesJSON) {
            dailyPrices.add(dailyPriceJson.convertToDailyPrice(preMarketPrices));
        }
        return dailyPrices;
    }

}
