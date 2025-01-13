package stock.price.analytics.cache;

import lombok.Getter;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.HighestLowestPrices;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HighLowPricesCache {

    private final Map<String, HighLow4w> highLow4wMap = new HashMap<>();
    private final Map<String, HighLow52Week> highLow52wMap = new HashMap<>();
    private final Map<String, HighestLowestPrices> highestLowestMap = new HashMap<>();
    @Getter
    private final List<HighLowForPeriod> newHighLowPrices = new ArrayList<>();

    public void addHighLowPrices(List<? extends HighLowForPeriod> hlPrices, HighLowPeriod highLowPeriod) {
        switch (highLowPeriod) {
            case HIGH_LOW_4W -> hlPrices.forEach(hlPrice -> highLow4wMap.merge(
                    hlPrice.getTicker(),
                    (HighLow4w) hlPrice,
                    (_, newPrice) -> newPrice
            ));
            case HIGH_LOW_52W -> hlPrices.forEach(hlPrice ->
                    highLow52wMap.merge(
                            hlPrice.getTicker(),
                            (HighLow52Week) hlPrice,
                            (_, newPrice) -> newPrice
                    )
            );
            case HIGH_LOW_ALL_TIME -> hlPrices.forEach(hlPrice ->
                    highestLowestMap.merge(
                            hlPrice.getTicker(),
                            (HighestLowestPrices) hlPrice,
                            (_, newPrice) -> newPrice
                    )
            );
        }
    }

    public List<? extends HighLowForPeriod> updateHighLowPricesCacheFrom(List<DailyPriceOHLC> dailyPricesImported, List<String> tickers, HighLowPeriod highLowPeriod) {
        Map<String, DailyPriceOHLC> dailyPricesImportedMap = dailyPricesImported.stream().collect(Collectors.toMap(DailyPriceOHLC::getTicker, p -> p));
        Map<String, ? extends HighLowForPeriod> highLowPrices = switch (highLowPeriod) {
            case HIGH_LOW_4W -> highLow4wMap;
            case HIGH_LOW_52W -> highLow52wMap;
            case HIGH_LOW_ALL_TIME -> highestLowestMap;
        };
        List<? extends HighLowForPeriod> updatedHighLowPrices = tickers.stream()
                .flatMap(ticker -> highLowPrices.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(ticker))
                        .map(Map.Entry::getValue)
                        .filter(hlp -> hlp.newHighLow(dailyPricesImportedMap.get(ticker))) // the method also assigns new high/low price not just return true/false
                ).collect(Collectors.toList());

        newHighLowPrices.addAll(updatedHighLowPrices); // used for stocks update high-low prices
        return updatedHighLowPrices;
    }

    // clean slate for next import
    public void clearNewHighLowPrices() {
        newHighLowPrices.clear();
    }
}
