package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCache;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.enums.MarketState;
import stock.price.analytics.repository.prices.DailyPricesRepository;

import java.util.List;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;

@Service
@RequiredArgsConstructor
public class DailyPricesCacheService {

    private final DailyPricesCache dailyPricesCache;
    private final DailyPricesRepository dailyPricesRepository;

    public void initDailyPricesCache() {
        dailyPricesCache.addDailyPrices(dailyPricesRepository.findLatestDailyPrices(), REGULAR);
    }

    protected List<DailyPrice> addDailyPricesInCacheAndReturn(List<DailyPrice> dailyPrices) {
        return dailyPricesCache.addDailyPricesInCacheAndReturn(dailyPrices);
    }

    protected List<DailyPrice> dailyPricesCache(MarketState marketState) {
        return dailyPricesCache.dailyPrices(marketState);
    }

    protected void addPreMarketDailyPricesInCache(List<DailyPrice> dailyPrices) {
        dailyPricesCache.addDailyPrices(dailyPrices, PRE);
    }
}