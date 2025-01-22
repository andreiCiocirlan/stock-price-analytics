package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesJSONCache;
import stock.price.analytics.model.prices.json.DailyPricesJSON;
import stock.price.analytics.repository.prices.DailyPricesJSONRepository;

import java.time.LocalDate;
import java.util.List;

import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Service
@RequiredArgsConstructor
public class DailyPricesJSONCacheService {

    private final DailyPricesJSONCache dailyPricesJSONCache;
    private final DailyPricesJSONRepository dailyPricesJSONRepository;

    // add last 7 trading days in  cache for good measure
    public void initDailyJSONPricesCache() {
        LocalDate tradingDateNow = tradingDateNow();
        dailyPricesJSONCache.addDailyJSONPrices(dailyPricesJSONRepository.findByDateBetween(tradingDateNow.minusDays(7), tradingDateNow));
    }

    public List<DailyPricesJSON> addDailyPricesJSONInCacheAndReturn(List<DailyPricesJSON> dailyPricesJSON) {
        return dailyPricesJSONCache.addDailyPricesJSONInCacheAndReturn(dailyPricesJSON);
    }

    public List<DailyPricesJSON> dailyPricesJSONCache() {
        return dailyPricesJSONCache.getDailyPricesJSONByTicker().values().stream().toList();
    }

}