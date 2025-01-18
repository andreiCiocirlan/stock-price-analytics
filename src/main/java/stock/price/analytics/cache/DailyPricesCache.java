package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DailyPricesCache {

    private final Map<String, DailyPriceOHLC> dailyPricesByTicker = new HashMap<>();

    public void addDailyPrices(List<DailyPriceOHLC> dailyPrices) {
        dailyPrices.forEach(price -> dailyPricesByTicker.merge(
                price.getTicker(),
                price,
                DailyPriceOHLC::updateFrom));
    }

    public List<DailyPriceOHLC> addDailyPricesInCacheAndReturn(List<DailyPriceOHLC> dailyPrices) {
        List<DailyPriceOHLC> addedPrices = new ArrayList<>();
        dailyPrices.forEach(price -> addToMap(price, addedPrices));
        return addedPrices;
    }

    private void addToMap(DailyPriceOHLC newPrice, List<DailyPriceOHLC> addedPrices) {
        DailyPriceOHLC existingPrice = dailyPricesByTicker.get(newPrice.getTicker());

        DailyPriceOHLC result = dailyPricesByTicker.merge(
                newPrice.getTicker(),
                newPrice,
                DailyPriceOHLC::updateFrom
        );

        // Check if a new price was added or replaced
        if (existingPrice == null || existingPrice.differentPrices(result)) {
            addedPrices.add(result);
        }
    }

    public List<DailyPriceOHLC> dailyPricesFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        dailyPricesByTicker.entrySet().stream()
                                .filter(entry -> entry.getKey().equals(ticker))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

}
