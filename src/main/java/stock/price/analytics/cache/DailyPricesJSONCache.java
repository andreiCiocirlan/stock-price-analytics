package stock.price.analytics.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.json.DailyPricesJSON;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DailyPricesJSONCache {

    private final List<String> inconsistentLows = new ArrayList<>();
    private final List<String> inconsistentHighs = new ArrayList<>();
    @Getter
    private final Map<String, DailyPricesJSON> dailyPricesJSONByTicker = new HashMap<>();

    public void addDailyJSONPrices(List<DailyPricesJSON> dailyPricesJSON) {
        dailyPricesJSON.forEach(price -> dailyPricesJSONByTicker.put(createKey(price.getSymbol(), price.getDate()), price));
    }

    public List<DailyPricesJSON> addDailyPricesJSONInCacheAndReturn(List<DailyPricesJSON> dailyPrices) {
        List<DailyPricesJSON> addedPrices = new ArrayList<>();
        dailyPrices.forEach(price -> addToMap(price, addedPrices));
        logInconsistentHighLowImportedPrices();
        return addedPrices;
    }

    private void addToMap(DailyPricesJSON newPrice, List<DailyPricesJSON> addedPrices) {
        DailyPricesJSON existingPrice = dailyPricesJSONByTicker.get(createKey(newPrice.getSymbol(), newPrice.getDate()));

        if (existingPrice != null) { // intraday update
            if (newPrice.getRegularMarketDayHigh() < existingPrice.getRegularMarketDayHigh()) {
                inconsistentLows.add(newPrice.getSymbol());
            } else {
                existingPrice.setRegularMarketDayHigh(newPrice.getRegularMarketDayHigh());
            }
            if (newPrice.getRegularMarketDayLow() > existingPrice.getRegularMarketDayLow()) {
                inconsistentHighs.add(newPrice.getSymbol());
            } else {
                existingPrice.setRegularMarketDayLow(newPrice.getRegularMarketDayLow());
            }
            existingPrice.setRegularMarketOpen(newPrice.getRegularMarketOpen());
            existingPrice.setRegularMarketPrice(newPrice.getRegularMarketPrice());
            existingPrice.setRegularMarketChangePercent(newPrice.getRegularMarketChangePercent());

            addedPrices.add(existingPrice);
        } else { // first import of the day
            dailyPricesJSONByTicker.put(createKey(newPrice.getSymbol(), newPrice.getDate()), newPrice);
            addedPrices.add(newPrice);
        }
    }

    private void logInconsistentHighLowImportedPrices() {
        if (!inconsistentHighs.isEmpty()) {
            log.warn("Inconsistent DAILY PRICES imported highs for {}", inconsistentHighs);
        }
        if (!inconsistentLows.isEmpty()) {
            log.warn("Inconsistent DAILY PRICES imported lows for {}", inconsistentLows);
        }
        inconsistentHighs.clear();
        inconsistentLows.clear();
    }

    private String createKey(String ticker, LocalDate date) {
        return ticker + "_" + date;
    }

}