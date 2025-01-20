package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCache;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.DailyPriceOHLCRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyPricesCacheService {

    private final DailyPricesCache dailyPricesCache;
    private final DailyPriceOHLCRepository dailyPriceOHLCRepository;

    public void initDailyPricesCache() {
        dailyPricesCache.addDailyPrices(dailyPriceOHLCRepository.findLatestDailyPrices());
    }

    public List<DailyPriceOHLC> addDailyPricesInCacheAndReturn(List<DailyPriceOHLC> dailyPriceOHLCs) {
        return dailyPricesCache.addDailyPricesInCacheAndReturn(dailyPriceOHLCs);
    }

    public List<DailyPriceOHLC> dailyPricesCache() {
        return dailyPricesCache.dailyPrices();
    }
}