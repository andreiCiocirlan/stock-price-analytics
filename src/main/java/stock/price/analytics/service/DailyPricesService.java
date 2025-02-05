package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.enums.MarketState;
import stock.price.analytics.repository.prices.DailyPricesRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyPricesService {

    private final DailyPricesCacheService dailyPricesCacheService;
    private final DailyPricesRepository dailyPricesRepository;

    public void initDailyPricesCache() {
        dailyPricesCacheService.initDailyPricesCache(dailyPricesRepository.findLatestDailyPrices());
    }

    public List<DailyPrice> dailyPricesCache(MarketState marketState) {
        return dailyPricesCacheService.dailyPricesCache(marketState);
    }

    public List<DailyPrice> addDailyPricesInCacheAndReturn(List<DailyPrice> dailyPrices) {
        return dailyPricesCacheService.addDailyPricesInCacheAndReturn(dailyPrices);
    }

    public void addPreMarketDailyPricesInCache(List<DailyPrice> preMarketPrices) {
        dailyPricesCacheService.addPreMarketDailyPricesInCache(preMarketPrices);
    }
}
