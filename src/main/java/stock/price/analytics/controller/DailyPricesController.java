package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.util.List;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;

@RequestMapping("/daily-prices")
@RestController
@RequiredArgsConstructor
public class DailyPricesController {

    private final CacheService cacheService;

    @GetMapping("/pre-market-cache")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> getPreMarketDailyPricesCache() {
        return cacheService.getCachedDailyPrices(PRE);
    }

}
