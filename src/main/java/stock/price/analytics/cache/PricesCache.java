package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.prices.ohlc.PriceWithPrevClose;

import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.enums.StockTimeframe.*;

@Component
class PricesCache {

    private final Map<StockTimeframe, Map<String, PriceWithPrevClose>> pricesWithPrevCloseByTimeframe = Map.of(
            DAILY, new HashMap<>(),
            WEEKLY, new HashMap<>(),
            MONTHLY, new HashMap<>(),
            QUARTERLY, new HashMap<>(),
            YEARLY, new HashMap<>()
    );

    private final Map<String, DailyPrice> preMarketDailyPricesByTicker = new HashMap<>();

    void addPreMarketPrices(List<DailyPrice> dailyPrices) {
        dailyPrices.forEach(price -> preMarketDailyPricesByTicker.put(price.getTicker(), price));
    }

    List<DailyPrice> preMarketPrices() {
        return new ArrayList<>(preMarketDailyPricesByTicker.values());
    }

    void addPricesWithPrevClose(List<PriceWithPrevClose> pricesWithPrevClose, StockTimeframe timeframe) {
        if (pricesWithPrevClose == null || pricesWithPrevClose.isEmpty())
            return; // Handle null or empty list case to avoid exceptions
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevCloseByTimeframe.get(timeframe);
        pricesWithPrevClose.forEach(p -> pricesWithPrevCloseByTicker.put(p.price().getTicker(), p));
    }

    List<PriceWithPrevClose> pricesWithPrevCloseFor(List<String> tickers, StockTimeframe timeframe) {
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevCloseByTimeframe.get(timeframe);
        return tickers.stream()
                .flatMap(ticker -> pricesWithPrevCloseByTicker.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(ticker))
                        .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    List<AbstractPrice> pricesFor(StockTimeframe timeframe) {
        return pricesWithPrevCloseByTimeframe.get(timeframe).values().stream()
                .map(PriceWithPrevClose::price)
                .toList();
    }
}