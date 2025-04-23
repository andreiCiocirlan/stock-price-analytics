package stock.price.analytics.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.enums.MarketState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;

@Slf4j
@Component
class DailyPriceCache {

    private final Map<String, DailyPrice> preMarketDailyPricesByTicker = new HashMap<>();
    private final Map<String, DailyPrice> dailyPricesByTicker = new HashMap<>();

    void addDailyPrices(List<DailyPrice> dailyPrices, MarketState marketState) {
        if (PRE == marketState) {
            dailyPrices.forEach(price -> preMarketDailyPricesByTicker.put(price.getTicker(), price));
        } else {
            dailyPrices.forEach(price -> dailyPricesByTicker.put(price.getTicker(), price));
        }
    }

    List<DailyPrice> cacheAndReturn(List<DailyPrice> dailyPrices) {
        List<DailyPrice> addedPrices = new ArrayList<>();
        dailyPrices.forEach(price -> addToMap(price, addedPrices));
        return addedPrices;
    }

    private void addToMap(DailyPrice newPrice, List<DailyPrice> addedPrices) {
        DailyPrice existingPrice = dailyPricesByTicker.get(newPrice.getTicker());

        if (existingPrice != null) {
            if (existingPrice.getDate().isEqual(newPrice.getDate())) { // intraday update
                existingPrice.setOpen(newPrice.getOpen());
                Pair<Double, Double> highLowPrices = getHighLowImportedPrices(newPrice, existingPrice);
                existingPrice.setHigh(highLowPrices.getLeft());
                existingPrice.setLow(highLowPrices.getRight());
                existingPrice.setClose(newPrice.getClose());
                existingPrice.setPerformance(newPrice.getPerformance());

                addedPrices.add(existingPrice);
            } else if (newPrice.getDate().isAfter(existingPrice.getDate())) { // newPrice must be newer compared to DB existing price (first daily import)
                dailyPricesByTicker.put(newPrice.getTicker(), newPrice);
                addedPrices.add(newPrice);
            }
        }
    }

    List<DailyPrice> dailyPrices(MarketState marketState) {
        if (PRE == marketState) {
            return new ArrayList<>(preMarketDailyPricesByTicker.values());
        }
        return new ArrayList<>(dailyPricesByTicker.values());
    }

    // utility method to find inconsistencies between imported high-low prices and already stored prices
    private Pair<Double, Double> getHighLowImportedPrices(DailyPrice importedDailyPrice, DailyPrice storedDailyPrice) {
        double high = importedDailyPrice.getHigh();
        double low = importedDailyPrice.getLow();
        // abnormal -> imported high price cannot be smaller than already stored high price
        if (importedDailyPrice.getHigh() < storedDailyPrice.getHigh()) {
            high = storedDailyPrice.getHigh();
        }
        // abnormal -> imported low price cannot be greater than already stored low price
        if (importedDailyPrice.getLow() > storedDailyPrice.getLow()) {
            low = storedDailyPrice.getLow();
        }
        return new MutablePair<>(high, low);
    }

}
