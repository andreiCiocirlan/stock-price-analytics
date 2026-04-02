package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;

import java.util.*;

@Component
final class CandleStickCache {

    private final Map<String, List<String>> tickersByCandleStickTypeAndTimeframe = new HashMap<>();

    void addTickerFor(CandleStickType candleStickType, StockTimeframe timeframe, String ticker) {
        tickersByCandleStickTypeAndTimeframe.computeIfAbsent(candleStickTypeKey(candleStickType, timeframe), _ -> new ArrayList<>()).add(ticker);
    }

    List<String> tickersFor(StockTimeframe timeframe, CandleStickType candleStickType) {
        return tickersByCandleStickTypeAndTimeframe.getOrDefault(candleStickTypeKey(candleStickType, timeframe), Collections.emptyList());
    }

    private String candleStickTypeKey(CandleStickType candleStickType, StockTimeframe timeframe) {
        return candleStickType + "_" + timeframe;
    }

    public void clearCache() {
        tickersByCandleStickTypeAndTimeframe.clear();
    }
}
