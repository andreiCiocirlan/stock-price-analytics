package stock.price.analytics.client.yahoo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.json.DailyPricesJSON;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.service.DailyPricesCacheService;
import stock.price.analytics.service.DailyPricesJSONService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.readAllLines;

@Component
@RequiredArgsConstructor
public class YahooFinanceClient {

    public final DailyPricesJSONService dailyPricesJSONService;
    public final DailyPricesCacheService dailyPricesCacheService;

    public List<DailyPrice> dailyPricesFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "C:\\Users/andre/IdeaProjects/stock-price-analytics/yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            return dailyPricesJSONService.dailyPricesFrom(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPrice> extractDailyPricesFromJSON(String jsonData) {
        List<DailyPricesJSON> dailyPricesJSON = dailyPricesJSONService.dailyPricesJSONFrom(jsonData);
        List<DailyPrice> preMarketPrices = dailyPricesJSON.stream()
                .filter(dp -> dp.getPreMarketPrice() != 0d)
                .map(dp -> dp.convertToDailyPrice(true))
                .toList();

        if (!preMarketPrices.isEmpty()) { // Cache premarket prices
            dailyPricesCacheService.addPreMarketDailyPricesInCache(preMarketPrices);
        }

        return dailyPricesJSONService.dailyPricesFrom(dailyPricesJSON);
    }

}
