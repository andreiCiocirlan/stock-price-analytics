package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.service.HighLowForPeriodService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/high-low")
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


}
