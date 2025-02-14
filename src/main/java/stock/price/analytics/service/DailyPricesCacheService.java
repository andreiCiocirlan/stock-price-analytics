package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCache;
import stock.price.analytics.cache.DailyPricesJSONCache;
import stock.price.analytics.model.prices.json.DailyPricesJSON;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.enums.MarketState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;

@Service
@RequiredArgsConstructor
public class DailyPricesCacheService {

    private final DailyPricesCache dailyPricesCache;
    private final DailyPricesJSONCache dailyPricesJSONCache;

    protected void initDailyPricesCache(List<DailyPrice> latestDailyPrices) {
        dailyPricesCache.addDailyPrices(latestDailyPrices, REGULAR);
    }
    protected void initPreviousDayPricesCache(List<DailyPrice> previousDayPrices) {
        dailyPricesCache.addPreviousDayPrices(previousDayPrices);
    }

    protected void initPreMarketDailyPricesCache() {
        Map<String, List<DailyPricesJSON>> dailyPricesJSONByTicker = dailyPricesJSONCache.getDailyPricesJSONByTicker().values().stream()
                .sorted(Comparator.comparing(DailyPricesJSON::getDate).reversed()) // order by date desc
                .collect(Collectors.groupingBy(DailyPricesJSON::getSymbol));

        List<DailyPrice> latestPreMarketDailyPrices = new ArrayList<>();

        for (List<DailyPricesJSON> dailyPricesJSONs : dailyPricesJSONByTicker.values()) {
            DailyPricesJSON latestPrice = dailyPricesJSONs.getFirst(); // take the first (latest) daily price per ticker
            if (latestPrice.getPreMarketPrice() != 0d) {
                latestPreMarketDailyPrices.add(latestPrice.convertToDailyPrice(true));
            }
        }
        addPreMarketDailyPricesInCache(latestPreMarketDailyPrices);
    }

    protected List<DailyPrice> addDailyPricesInCacheAndReturn(List<DailyPrice> dailyPrices) {
        return dailyPricesCache.addDailyPricesInCacheAndReturn(dailyPrices);
    }

    protected List<DailyPrice> dailyPricesCache(MarketState marketState) {
        return dailyPricesCache.dailyPrices(marketState);
    }

    protected List<DailyPrice> previousDailyPrices() {
        return dailyPricesCache.previousDailyPrices();
    }

    protected void addPreMarketDailyPricesInCache(List<DailyPrice> preMarketDailyPrices) {
        dailyPricesCache.addDailyPrices(preMarketDailyPrices, PRE);
    }
}