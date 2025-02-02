package stock.price.analytics.cache;

import lombok.Getter;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.enums.PriceMilestone;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.HighestLowestPrices;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class HighLowPricesCache {

    private final Map<String, HighLow4w> highLow4wMap = new HashMap<>();
    private final Map<String, HighLow52Week> highLow52wMap = new HashMap<>();
    private final Map<String, HighestLowestPrices> highestLowestMap = new HashMap<>();
    private final Map<String, HighLow4w> prevWeekHighLow4wMap = new HashMap<>();
    private final Map<String, HighLow52Week> prevWeekHighLow52wMap = new HashMap<>();
    private final Map<String, HighestLowestPrices> prevWeekHighestLowestMap = new HashMap<>();
    @Getter
    private final Map<HighLowPeriod, Set<String>> dailyNewHighLowsByHLPeriod = new HashMap<>();

    public void addPrevWeekHighLowPrices(List<? extends HighLowForPeriod> prevWeekHighLowPrices, HighLowPeriod highLowPeriod) {
        switch (highLowPeriod) {
            case HIGH_LOW_4W -> prevWeekHighLowPrices.forEach(hlPrice ->
                    prevWeekHighLow4wMap.merge(
                            hlPrice.getTicker(),
                            (HighLow4w) hlPrice,
                            (_, newPrice) -> newPrice
                    ));
            case HIGH_LOW_52W -> prevWeekHighLowPrices.forEach(hlPrice ->
                    prevWeekHighLow52wMap.merge(
                            hlPrice.getTicker(),
                            (HighLow52Week) hlPrice,
                            (_, newPrice) -> newPrice
                    )
            );
            case HIGH_LOW_ALL_TIME -> prevWeekHighLowPrices.forEach(hlPrice ->
                    prevWeekHighestLowestMap.merge(
                            hlPrice.getTicker(),
                            (HighestLowestPrices) hlPrice,
                            (_, newPrice) -> newPrice
                    )
            );
        }
    }

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

    public List<? extends HighLowForPeriod> updateHighLowPricesCacheFrom(List<DailyPrice> dailyPrices, List<String> tickers, HighLowPeriod highLowPeriod) {
        Map<String, DailyPrice> dailyPricesByTicker = dailyPrices.stream().collect(Collectors.toMap(DailyPrice::getTicker, p -> p));
        Map<String, ? extends HighLowForPeriod> highLowPrices = switch (highLowPeriod) {
            case HIGH_LOW_4W -> highLow4wMap;
            case HIGH_LOW_52W -> highLow52wMap;
            case HIGH_LOW_ALL_TIME -> highestLowestMap;
        };
        List<? extends HighLowForPeriod> updatedHighLowPrices = tickers.stream()
                .flatMap(ticker -> highLowPrices.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(ticker))
                        .map(Map.Entry::getValue)
                        .filter(hlp -> hlp.newHighLow(dailyPricesByTicker.get(ticker))) // the method also assigns new high/low price not just return true/false
                ).collect(Collectors.toList());

        // add new highs and lows for 4w, 52w, all-time into cache (to be printed on-demand)
        for (HighLowForPeriod newHighLowPrice : updatedHighLowPrices) {
            dailyNewHighLowsByHLPeriod.computeIfAbsent(newHighLowPrice.getHighLowPeriod(), _ -> new HashSet<>()).add(newHighLowPrice.getTicker());
        }

        return updatedHighLowPrices;
    }

    public List<? extends HighLowForPeriod> cacheForHighLowPeriod(HighLowPeriod highLowPeriod) {
        return switch (highLowPeriod) {
            case HIGH_LOW_4W -> new ArrayList<>(highLow4wMap.values());
            case HIGH_LOW_52W -> new ArrayList<>(highLow52wMap.values());
            case HIGH_LOW_ALL_TIME -> new ArrayList<>(highestLowestMap.values());
        };
    }

    public List<? extends HighLowForPeriod> cacheForMilestone(PriceMilestone priceMilestone) {
        return switch (priceMilestone) {
            case HIGH_4W_95, LOW_4W_95 -> new ArrayList<>(highLow4wMap.values());
            case NEW_4W_HIGH, NEW_4W_LOW -> new ArrayList<>(prevWeekHighLow4wMap.values());
            case HIGH_52W_95, LOW_52W_95 -> new ArrayList<>(highLow52wMap.values());
            case NEW_52W_HIGH, NEW_52W_LOW -> new ArrayList<>(prevWeekHighLow52wMap.values());
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95 -> new ArrayList<>(highestLowestMap.values());
            case NEW_ALL_TIME_HIGH, NEW_ALL_TIME_LOW -> new ArrayList<>(prevWeekHighestLowestMap.values());
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }
}
