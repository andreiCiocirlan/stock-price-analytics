package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCache;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.DailyPricesRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyPricesCacheService {

    private final DailyPricesCache dailyPricesCache;
    private final DailyPricesRepository dailyPricesRepository;

    public void initDailyPricesCache() {
        dailyPricesCache.addDailyPrices(dailyPricesRepository.findLatestDailyPrices());
    }

    public List<DailyPriceOHLC> addDailyPricesInCacheAndReturn(List<DailyPriceOHLC> dailyPrices) {
        return dailyPricesCache.addDailyPricesInCacheAndReturn(dailyPrices);
    }

    public List<DailyPriceOHLC> dailyPricesCache() {
        return dailyPricesCache.dailyPrices();
    }
}