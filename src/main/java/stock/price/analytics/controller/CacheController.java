package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.util.ArrayList;
import java.util.List;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cache")
public class CacheController {

    private final CacheService cacheService;

    @GetMapping("/pre-market-prices")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> getPreMarketDailyPricesCache() {
        return cacheService.getCachedDailyPrices(PRE);
    }

    @GetMapping("/new-high-lows")
    @ResponseStatus(HttpStatus.OK)
    public List<String> newDailyHighLowsForHLPeriods(@RequestParam("highLowPeriod") HighLowPeriod highLowPeriod) {
        return cacheService.getNewHighLowsForHLPeriod(highLowPeriod);
    }

    @GetMapping("/equal-high-lows")
    @ResponseStatus(HttpStatus.OK)
    public List<String> equalHighLowsForHLPeriods(@RequestParam("highLowPeriod") HighLowPeriod highLowPeriod) {
        return cacheService.getEqualHighLowsForHLPeriod(highLowPeriod);
    }

    @GetMapping("/high-lows")
    @ResponseStatus(HttpStatus.OK)
    public List<? extends HighLowForPeriod> highLowsForTicker(@RequestParam("ticker") String ticker) {
        List<HighLowForPeriod> result = new ArrayList<>();
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            result.add(cacheService.highLowForPeriodPricesFor(highLowPeriod).stream().filter(hlp -> ticker.equals(hlp.getTicker())).findFirst().orElseThrow());
        }
        return result;
    }

}
