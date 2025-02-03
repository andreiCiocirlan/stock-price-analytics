package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DailyPricesCache {

    private final Map<String, DailyPrice> dailyPricesByTicker = new HashMap<>();

    public void addDailyPrices(List<DailyPrice> dailyPrices) {
        dailyPrices.forEach(price -> dailyPricesByTicker.put(
                price.getTicker(),
                price));
    }

    public List<DailyPrice> addDailyPricesInCacheAndReturn(List<DailyPrice> dailyPrices) {
        List<DailyPrice> addedPrices = new ArrayList<>();
        dailyPrices.forEach(price -> addToMap(price, addedPrices));
        return addedPrices;
    }

    private void addToMap(DailyPrice newPrice, List<DailyPrice> addedPrices) {
        DailyPrice existingPrice = dailyPricesByTicker.get(newPrice.getTicker());

        if (existingPrice != null) {
            if (existingPrice.getDate().isEqual(newPrice.getDate())) { // intraday update
                existingPrice.setOpen(newPrice.getOpen());
                existingPrice.setHigh(newPrice.getHigh());
                existingPrice.setLow(newPrice.getLow());
                existingPrice.setClose(newPrice.getClose());
                existingPrice.setPerformance(newPrice.getPerformance());

                addedPrices.add(existingPrice);
            } else if (newPrice.getDate().isAfter(existingPrice.getDate())) { // newPrice must be newer compared to DB existing price (first daily import)
                dailyPricesByTicker.put(newPrice.getTicker(), newPrice);
                addedPrices.add(newPrice);
            }
        }
    }

    public List<DailyPrice> dailyPrices() {
        return new ArrayList<>(dailyPricesByTicker.values());
    }

}
