package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.model.prices.enums.StockPerformanceInterval;
import stock.price.analytics.service.HighLowForPeriodService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class HighLowForPeriodController {

    private final HighLowForPeriodService highLowForPeriodService;

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

    @PostMapping("/save-hl-4w-52w-ticker")
    @ResponseStatus(HttpStatus.OK)
    public void saveHighLow_4w_52wForTickerAndDate(@RequestParam("ticker") String ticker,
                                                       @RequestParam(name = "tradingDate")
                                                            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradingDate) {
        highLowForPeriodService.saveCurrentWeekHighLowPrices(List.of(ticker), tradingDate);
    }

}
