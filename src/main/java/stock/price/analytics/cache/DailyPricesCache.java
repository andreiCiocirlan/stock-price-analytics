package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.enums.MarketState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;

@Component
public class DailyPricesCache {

    private final Map<String, DailyPrice> preMarketDailyPricesByTicker = new HashMap<>();
    private final Map<String, DailyPrice> dailyPricesByTicker = new HashMap<>();
    private final Map<String, DailyPrice> previousDayPricesByTicker = new HashMap<>();

    public void addDailyPrices(List<DailyPrice> dailyPrices, MarketState marketState) {
        if (PRE == marketState) {
            dailyPrices.forEach(price -> preMarketDailyPricesByTicker.put(price.getTicker(), price));
        } else {
            dailyPrices.forEach(price -> dailyPricesByTicker.put(price.getTicker(), price));
        }
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

    public List<DailyPrice> dailyPrices(MarketState marketState) {
        if (PRE == marketState) {
            return new ArrayList<>(preMarketDailyPricesByTicker.values());
        }
        return new ArrayList<>(dailyPricesByTicker.values());
    }

    public void addPreviousDayPrices(List<DailyPrice> previousDayPrices) {
        previousDayPrices.forEach(price -> previousDayPricesByTicker.put(price.getTicker(), price));
    }

    public List<DailyPrice> previousDailyPrices() {
        return new ArrayList<>(previousDayPricesByTicker.values());
    }

}
