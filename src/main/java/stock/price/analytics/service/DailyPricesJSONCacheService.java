package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesJSONCache;
import stock.price.analytics.model.prices.json.DailyPricesJSON;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyPricesJSONCacheService {

    private final DailyPricesJSONCache dailyPricesJSONCache;

    protected void initDailyJSONPricesCache(List<DailyPricesJSON> latestDailyPricesJSON) {
        dailyPricesJSONCache.addDailyJSONPrices(latestDailyPricesJSON);
    }

    protected List<DailyPricesJSON> addDailyPricesJSONInCacheAndReturn(List<DailyPricesJSON> dailyPricesJSON) {
        return dailyPricesJSONCache.addDailyPricesJSONInCacheAndReturn(dailyPricesJSON);
    }

    protected List<DailyPricesJSON> dailyPricesJSONCache() {
        return dailyPricesJSONCache.getDailyPricesJSONByTicker().values().stream().toList();
    }

}