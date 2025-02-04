package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.util.List;

import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;

@Service
@RequiredArgsConstructor
public class DailyPricesService {

    private final DailyPricesCacheService dailyPricesCacheService;

    public List<DailyPrice> dailyPricesCache() {
        return dailyPricesCacheService.dailyPricesCache(REGULAR);
    }

    public List<DailyPrice> addDailyPricesInCacheAndReturn(List<DailyPrice> dailyPrices) {
        return dailyPricesCacheService.addDailyPricesInCacheAndReturn(dailyPrices);
    }

}
