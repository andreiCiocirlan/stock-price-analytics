package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCache;
import stock.price.analytics.repository.prices.DailyPriceOHLCRepository;

@Service
@RequiredArgsConstructor
public class DailyPricesCacheService {

    private final DailyPricesCache dailyPricesCache;
    private final DailyPriceOHLCRepository dailyPriceOHLCRepository;

    public void initDailyPricesCache() {
        dailyPricesCache.addDailyPrices(dailyPriceOHLCRepository.findXTBLatestDailyPricesImported());
    }

}