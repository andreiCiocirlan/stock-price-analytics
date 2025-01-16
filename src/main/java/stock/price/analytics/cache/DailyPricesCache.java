package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DailyPricesCache {

    private final Map<String, DailyPriceOHLC> dailyPricesByTicker = new HashMap<>();

    public void addDailyPrices(List<DailyPriceOHLC> dailyPrices) {
        dailyPrices.forEach(price -> {
            this.dailyPricesByTicker.merge(
                    price.getTicker(),
                    price,
                    (existingPrice, newPrice) -> existingPrice.updateFrom(newPrice, price.getDate()));
        });
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
