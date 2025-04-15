package stock.price.analytics.cache;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.NewHighLowMilestone;
import stock.price.analytics.model.prices.enums.PerformanceMilestone;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.HighestLowestPrices;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.util.*;
import java.util.stream.Collectors;

@Component
class HighLowPricesCache {

    @Getter @Setter
    private Boolean weeklyHighLowExists;
    private final Map<String, HighLow4w> highLow4wMap = new HashMap<>();
    private final Map<String, HighLow52Week> highLow52wMap = new HashMap<>();
    private final Map<String, HighestLowestPrices> highestLowestMap = new HashMap<>();
    private final Map<String, HighLow4w> prevWeekHighLow4wMap = new HashMap<>();
    private final Map<String, HighLow52Week> prevWeekHighLow52wMap = new HashMap<>();
    private final Map<String, HighestLowestPrices> prevWeekHighestLowestMap = new HashMap<>();
    @Getter
    private final Map<HighLowPeriod, Set<String>> dailyNewHighLowsByHLPeriod = new HashMap<>();
    @Getter
    private final Map<HighLowPeriod, Set<String>> dailyEqualHighLowsByHLPeriod = new HashMap<>();

    void addPrevWeekHighLowPrices(List<? extends HighLowForPeriod> prevWeekHighLowPrices, HighLowPeriod highLowPeriod) {
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

    void addHighLowPrices(List<? extends HighLowForPeriod> hlPrices, HighLowPeriod highLowPeriod) {
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

    List<? extends HighLowForPeriod> getUpdatedHighLowPricesForTickers(List<DailyPrice> dailyPrices, List<String> tickers, HighLowPeriod highLowPeriod) {
        Map<String, DailyPrice> dailyPricesByTicker = dailyPrices.stream().collect(Collectors.toMap(DailyPrice::getTicker, p -> p));
        Map<String, ? extends HighLowForPeriod> highLowPrices = switch (highLowPeriod) {
            case HIGH_LOW_4W -> highLow4wMap;
            case HIGH_LOW_52W -> highLow52wMap;
            case HIGH_LOW_ALL_TIME -> highestLowestMap;
        };
        return tickers.stream()
                .flatMap(ticker -> highLowPrices.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(ticker))
                        .map(Map.Entry::getValue)
                        .filter(hlp -> isNewHighLowOrEqualFor(ticker, hlp, dailyPricesByTicker))
                ).collect(Collectors.toList());
    }

    private boolean isNewHighLowOrEqualFor(String ticker, HighLowForPeriod hlp, Map<String, DailyPrice> dailyPricesByTicker) {
        Pair<Boolean, Boolean> newHighLowOrEqual = hlp.newHighLowOrEqual(dailyPricesByTicker.get(ticker)); // this also assigns new high/low price not just return true/false
        boolean isNewHighLow = newHighLowOrEqual.getLeft();
        boolean isEqualHighLow = newHighLowOrEqual.getRight();
        if (isNewHighLow) {
            dailyNewHighLowsByHLPeriod.computeIfAbsent(hlp.getHighLowPeriod(), _ -> new HashSet<>()).add(hlp.getTicker());
        }
        if (isEqualHighLow) {
            dailyEqualHighLowsByHLPeriod.computeIfAbsent(hlp.getHighLowPeriod(), _ -> new HashSet<>()).add(hlp.getTicker());
        }
        return isNewHighLow;
    }

    List<? extends HighLowForPeriod> cacheForHighLowPeriod(HighLowPeriod highLowPeriod, boolean prevWeek) {
        return new ArrayList<>(switch (highLowPeriod) {
            case HIGH_LOW_4W -> prevWeek ? prevWeekHighLow4wMap.values() : highLow4wMap.values();
            case HIGH_LOW_52W -> prevWeek ? prevWeekHighLow52wMap.values() : highLow52wMap.values();
            case HIGH_LOW_ALL_TIME -> prevWeek ? prevWeekHighestLowestMap.values() : highestLowestMap.values();
        });
    }

    List<? extends HighLowForPeriod> cacheForNewHighLowMilestone(NewHighLowMilestone newHighLowMilestone) {
        return new ArrayList<>(switch (newHighLowMilestone) {
            case NEW_4W_HIGH, NEW_4W_LOW -> prevWeekHighLow4wMap.values();
            case NEW_52W_HIGH, NEW_52W_LOW -> prevWeekHighLow52wMap.values();
            case NEW_ALL_TIME_HIGH, NEW_ALL_TIME_LOW -> prevWeekHighestLowestMap.values();
        });
    }

    List<? extends HighLowForPeriod> cacheForPricePerformanceMilestone(PricePerformanceMilestone pricePerformanceMilestone) {
        return new ArrayList<>(switch (pricePerformanceMilestone) {
            case HIGH_4W_95, LOW_4W_95 -> highLow4wMap.values();
            case HIGH_52W_95, LOW_52W_95 -> highLow52wMap.values();
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95 -> highestLowestMap.values();
        });
    }
}
