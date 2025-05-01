package stock.price.analytics.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static stock.price.analytics.model.prices.enums.StockTimeframe.*;

@Getter
@Component
public class CandleStickCache {

    @Setter
    private Map<String, Double> avgCandleRangesByTickerAndTimeframe = new HashMap<>();
    private final Map<StockTimeframe, Map<CandleStickType, List<String>>> candleStickTypeByTickers = Map.of(
            DAILY, new HashMap<>(),
            WEEKLY, new HashMap<>(),
            MONTHLY, new HashMap<>(),
            QUARTERLY, new HashMap<>(),
            YEARLY, new HashMap<>()
    );


    public Double getFor(String ticker, StockTimeframe timeframe) {
        return avgCandleRangesByTickerAndTimeframe.getOrDefault(ticker + "_" + timeframe, 0d);
    }
}
