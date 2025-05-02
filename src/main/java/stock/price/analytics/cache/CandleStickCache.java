package stock.price.analytics.cache;

import lombok.Getter;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;

import java.util.*;

@Getter
@Component
public class CandleStickCache {

    private final Map<String, Double> avgCandleRangesByTickerAndTimeframe = new HashMap<>();
    private final Map<String, List<String>> tickersByCandleStickTypeAndTimeframe = new HashMap<>();

    void addAvgCandleRangeFor(String ticker, StockTimeframe timeframe, Double range) {
        avgCandleRangesByTickerAndTimeframe.put(ticker + "_" + timeframe, range);
    }

    Double averageCandleRangeFor(String ticker, StockTimeframe timeframe) {
        return avgCandleRangesByTickerAndTimeframe.getOrDefault(ticker + "_" + timeframe, 0d);
    }

    void addTickerFor(CandleStickType type, StockTimeframe timeframe, String ticker) {
        String key = type + "_" + timeframe;
        tickersByCandleStickTypeAndTimeframe.computeIfAbsent(key, _ -> new ArrayList<>()).add(ticker);
    }

    List<String> tickersFor(StockTimeframe timeframe, CandleStickType candleStickType) {
        return tickersByCandleStickTypeAndTimeframe.getOrDefault(candleStickType + "_" + timeframe, Collections.emptyList());
    }

}
