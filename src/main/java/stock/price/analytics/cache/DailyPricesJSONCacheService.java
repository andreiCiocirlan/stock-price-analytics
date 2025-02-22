package stock.price.analytics.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.json.DailyPricesJSON;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyPricesJSONCacheService {

    private final DailyPricesJSONCache dailyPricesJSONCache;

    public void initDailyJSONPricesCache(List<DailyPricesJSON> latestDailyPricesJSON) {
        dailyPricesJSONCache.addDailyJSONPrices(latestDailyPricesJSON);
    }

    public List<DailyPricesJSON> addDailyPricesJSONInCacheAndReturn(List<DailyPricesJSON> dailyPricesJSON) {
        return dailyPricesJSONCache.addDailyPricesJSONInCacheAndReturn(dailyPricesJSON);
    }

    public List<DailyPricesJSON> dailyPricesJSONCache() {
        return dailyPricesJSONCache.getDailyPricesJSONByTicker().values().stream().toList();
    }

}