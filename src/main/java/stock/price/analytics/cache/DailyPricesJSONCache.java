package stock.price.analytics.cache;

import lombok.Getter;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.json.DailyPricesJSON;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Component
class DailyPricesJSONCache {

    private final Map<String, DailyPricesJSON> dailyPricesJSONByTicker = new HashMap<>();

    void addDailyJSONPrices(List<DailyPricesJSON> dailyPricesJSON) {
        dailyPricesJSON.forEach(price -> dailyPricesJSONByTicker.put(createKey(price.getSymbol(), price.getDate()), price));
    }

    List<DailyPricesJSON> addDailyPricesJSONInCacheAndReturn(List<DailyPricesJSON> dailyPrices) {
        List<DailyPricesJSON> addedPrices = new ArrayList<>();
        dailyPrices.forEach(price -> addToMap(price, addedPrices));
        return addedPrices;
    }

    private void addToMap(DailyPricesJSON newPrice, List<DailyPricesJSON> addedPrices) {
        DailyPricesJSON existingPrice = dailyPricesJSONByTicker.get(createKey(newPrice.getSymbol(), newPrice.getDate()));

        if (existingPrice != null) { // intraday update
            existingPrice.setRegularMarketOpen(newPrice.getRegularMarketOpen());
            existingPrice.setRegularMarketDayHigh(newPrice.getRegularMarketDayHigh());
            existingPrice.setRegularMarketDayLow(newPrice.getRegularMarketDayLow());
            existingPrice.setRegularMarketPrice(newPrice.getRegularMarketPrice());
            existingPrice.setRegularMarketChangePercent(newPrice.getRegularMarketChangePercent());

            addedPrices.add(existingPrice);
        } else { // first import of the day
            dailyPricesJSONByTicker.put(createKey(newPrice.getSymbol(), newPrice.getDate()), newPrice);
            addedPrices.add(newPrice);
        }
    }

    private String createKey(String ticker, LocalDate date) {
        return ticker + "_" + date;
    }

}