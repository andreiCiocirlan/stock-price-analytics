package stock.price.analytics.cache;

import lombok.Getter;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.json.DailyPriceJSON;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Component
class DailyPriceJsonCache {

    private final Map<String, DailyPriceJSON> dailyPriceJSONByTicker = new HashMap<>();

    void addDailyJSONPrices(List<DailyPriceJSON> dailyPriceJSONs) {
        dailyPriceJSONs.forEach(price -> dailyPriceJSONByTicker.put(createKey(price.getSymbol(), price.getDate()), price));
    }

    List<DailyPriceJSON> cacheAndReturn(List<DailyPriceJSON> dailyPriceJSONs) {
        List<DailyPriceJSON> addedPrices = new ArrayList<>();
        dailyPriceJSONs.forEach(price -> addToMap(price, addedPrices));
        return addedPrices;
    }

    private void addToMap(DailyPriceJSON newPrice, List<DailyPriceJSON> addedPrices) {
        DailyPriceJSON existingPrice = dailyPriceJSONByTicker.get(createKey(newPrice.getSymbol(), newPrice.getDate()));

        if (existingPrice != null) { // intraday update
            existingPrice.setRegularMarketOpen(newPrice.getRegularMarketOpen());
            existingPrice.setRegularMarketDayHigh(newPrice.getRegularMarketDayHigh());
            existingPrice.setRegularMarketDayLow(newPrice.getRegularMarketDayLow());
            existingPrice.setRegularMarketPrice(newPrice.getRegularMarketPrice());
            existingPrice.setRegularMarketChangePercent(newPrice.getRegularMarketChangePercent());

            addedPrices.add(existingPrice);
        } else { // first import of the day
            dailyPriceJSONByTicker.put(createKey(newPrice.getSymbol(), newPrice.getDate()), newPrice);
            addedPrices.add(newPrice);
        }
    }

    private String createKey(String ticker, LocalDate date) {
        return ticker + "_" + date;
    }

}