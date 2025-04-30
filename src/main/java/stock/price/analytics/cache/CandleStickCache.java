package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;

import java.util.HashMap;
import java.util.Map;

@Component
public class CandleStickCache {

    private final Map<String, Double> avgCandleLength15Days = new HashMap<>();
    private final Map<String, CandleStickType> candleStickTypeByTickers = new HashMap<>();



}
