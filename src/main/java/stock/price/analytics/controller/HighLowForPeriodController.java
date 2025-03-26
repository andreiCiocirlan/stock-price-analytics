package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.service.HighLowForPeriodService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HighLowForPeriodController {

    private final HighLowForPeriodService highLowForPeriodService;
    private final CacheService cacheService;

    @PostMapping("/save-all-hl-4w-52w-ticker")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void saveAllHistoricalHighLowPrices(@RequestParam("ticker") String ticker,
                                                       @RequestParam(name = "tradingDate")
                                                            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradingDate) {
        highLowForPeriodService.saveAllHistoricalHighLowPrices(List.of(ticker), tradingDate);
    }

    @GetMapping("/daily-new-high-lows-for-hl-period")
    @ResponseStatus(HttpStatus.OK)
    public void newDailyHighLowsForHLPeriods() {
        cacheService.logNewHighLowsForHLPeriods();
    }

    @GetMapping("/daily-equal-high-lows-for-hl-period")
    @ResponseStatus(HttpStatus.OK)
    public void equalHighLowsForHLPeriods() {
        cacheService.logEqualHighLowsForHLPeriods();
    }

    @GetMapping("/high-lows-for-ticker")
    @ResponseStatus(HttpStatus.OK)
    public List<? extends HighLowForPeriod> highLowsForTicker(@RequestParam("ticker") String ticker) {
        List<HighLowForPeriod> result = new ArrayList<>();
        result.add(cacheService.highLowForPeriodPricesFor(HighLowPeriod.HIGH_LOW_4W).stream().filter(hlp -> ticker.equals(hlp.getTicker())).findFirst().orElseThrow());
        result.add(cacheService.highLowForPeriodPricesFor(HighLowPeriod.HIGH_LOW_52W).stream().filter(hlp -> ticker.equals(hlp.getTicker())).findFirst().orElseThrow());
        result.add(cacheService.highLowForPeriodPricesFor(HighLowPeriod.HIGH_LOW_ALL_TIME).stream().filter(hlp -> ticker.equals(hlp.getTicker())).findFirst().orElseThrow());
        return result;
    }

}
