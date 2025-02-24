package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.cache.HighLowPricesCacheService;
import stock.price.analytics.model.prices.enums.StockPerformanceInterval;
import stock.price.analytics.service.HighLowForPeriodService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HighLowForPeriodController {

    private final HighLowForPeriodService highLowForPeriodService;
    private final HighLowPricesCacheService highLowPricesCacheService;

    @PostMapping("/save-52w-high-low")
    @ResponseStatus(HttpStatus.OK)
    public void save52WeekHighLowPrices() {
        highLowForPeriodService.saveHighLowPricesForPeriod(StockPerformanceInterval.STOCK_PERF_INTERVAL_52W);
    }

    @PostMapping("/save-30d-high-low")
    @ResponseStatus(HttpStatus.OK)
    public void save30DayHighLowPrices() {
        highLowForPeriodService.saveHighLowPricesForPeriod(StockPerformanceInterval.STOCK_PERF_INTERVAL_30D);
    }

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
        highLowPricesCacheService.logNewHighLowsForHLPeriods();
    }

    @GetMapping("/daily-equal-high-lows-for-hl-period")
    @ResponseStatus(HttpStatus.OK)
    public void equalHighLowsForHLPeriods() {
        highLowPricesCacheService.logEqualHighLowsForHLPeriods();
    }

}
