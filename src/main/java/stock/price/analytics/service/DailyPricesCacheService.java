package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCache;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.enums.MarketState;

import java.util.List;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;

@Service
@RequiredArgsConstructor
public class DailyPricesCacheService {

    private final DailyPricesCache dailyPricesCache;

    protected void initDailyPricesCache(List<DailyPrice> latestDailyPrices) {
        dailyPricesCache.addDailyPrices(latestDailyPrices, REGULAR);
    }

    protected List<DailyPrice> addDailyPricesInCacheAndReturn(List<DailyPrice> dailyPrices) {
        return dailyPricesCache.addDailyPricesInCacheAndReturn(dailyPrices);
    }

    protected List<DailyPrice> dailyPricesCache(MarketState marketState) {
        return dailyPricesCache.dailyPrices(marketState);
    }

    protected void addPreMarketDailyPricesInCache(List<DailyPrice> preMarketDailyPrices) {
        dailyPricesCache.addDailyPrices(preMarketDailyPrices, PRE);
    }
}