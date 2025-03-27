package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cache")
public class CacheController {

    private final CacheService cacheService;

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
        result.add(cacheService.highLowForPeriodPricesFor(HighLowPeriod.HIGH_LOW_4W).stream().filter(hlp -> ticker.equals(hlp.getTicker())).findFirst().orElseThrow());
        result.add(cacheService.highLowForPeriodPricesFor(HighLowPeriod.HIGH_LOW_52W).stream().filter(hlp -> ticker.equals(hlp.getTicker())).findFirst().orElseThrow());
        result.add(cacheService.highLowForPeriodPricesFor(HighLowPeriod.HIGH_LOW_ALL_TIME).stream().filter(hlp -> ticker.equals(hlp.getTicker())).findFirst().orElseThrow());
        return result;
    }

}
