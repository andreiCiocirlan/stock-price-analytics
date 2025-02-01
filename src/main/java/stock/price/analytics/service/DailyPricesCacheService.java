package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCache;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
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

    public List<DailyPrice> addDailyPricesInCacheAndReturn(List<DailyPrice> dailyPrices) {
        return dailyPricesCache.addDailyPricesInCacheAndReturn(dailyPrices);
    }

    public List<DailyPrice> dailyPricesCache() {
        return dailyPricesCache.dailyPrices();
    }
}