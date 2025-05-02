package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;

import java.util.*;

@Component
public class CandleStickCache {

    private final Map<String, Double> avgCandleRangesByTickerAndTimeframe = new HashMap<>();
    private final Map<String, List<String>> tickersByCandleStickTypeAndTimeframe = new HashMap<>();

    void addAvgCandleRangeFor(String ticker, StockTimeframe timeframe, Double range) {
        avgCandleRangesByTickerAndTimeframe.put(avgCandleRangeKey(ticker, timeframe), range);
    }

    Double averageCandleRangeFor(String ticker, StockTimeframe timeframe) {
        return avgCandleRangesByTickerAndTimeframe.getOrDefault(avgCandleRangeKey(ticker, timeframe), 0d);
    }

    void addTickerFor(CandleStickType candleStickType, StockTimeframe timeframe, String ticker) {
        tickersByCandleStickTypeAndTimeframe.computeIfAbsent(candleStickTypeKey(candleStickType, timeframe), _ -> new ArrayList<>()).add(ticker);
    }

    List<String> tickersFor(StockTimeframe timeframe, CandleStickType candleStickType) {
        return tickersByCandleStickTypeAndTimeframe.getOrDefault(candleStickTypeKey(candleStickType, timeframe), Collections.emptyList());
    }

    private String avgCandleRangeKey(String ticker, StockTimeframe timeframe) {
        return ticker + "_" + timeframe;
    }

    private String candleStickTypeKey(CandleStickType candleStickType, StockTimeframe timeframe) {
        return candleStickType + "_" + timeframe;
    }

}
