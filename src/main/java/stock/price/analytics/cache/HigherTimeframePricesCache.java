package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.PriceWithPrevClose;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.enums.StockTimeframe.*;

@Component
class HigherTimeframePricesCache {

    private final Map<StockTimeframe, Map<String, PriceWithPrevClose>> pricesWithPrevCloseByTimeframe = Map.of(
            WEEKLY, new HashMap<>(),
            MONTHLY, new HashMap<>(),
            QUARTERLY, new HashMap<>(),
            YEARLY, new HashMap<>()
    );

    void addPricesWithPrevClose(List<PriceWithPrevClose> pricesWithPrevClose, StockTimeframe timeframe) {
        if (pricesWithPrevClose == null || pricesWithPrevClose.isEmpty())
            return; // Handle null or empty list case to avoid exceptions
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevCloseByTimeframe.get(timeframe);
        pricesWithPrevClose.forEach(price -> pricesWithPrevCloseByTicker.put(price.abstractPrice().getTicker(), price));
    }

    List<PriceWithPrevClose> pricesWithPrevCloseFor(List<String> tickers, StockTimeframe timeframe) {
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevCloseByTimeframe.get(timeframe);
        return tickers.stream()
                .flatMap(ticker -> pricesWithPrevCloseByTicker.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(ticker))
                        .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    List<AbstractPrice> htfPricesFor(StockTimeframe timeframe) {
        return pricesWithPrevCloseByTimeframe.get(timeframe).values().stream()
                .map(PriceWithPrevClose::abstractPrice)
                .toList();
    }
}