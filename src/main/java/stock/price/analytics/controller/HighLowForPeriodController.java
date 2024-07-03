package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.model.prices.enums.StockPerformanceInterval;
import stock.price.analytics.service.HighLowForPeriodService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class HighLowForPeriodController {

    private final HighLowForPeriodService highLowForPeriodService;

    @PostMapping("/save-52w-high-low")
    @ResponseStatus(HttpStatus.OK)
    public void save52WeekHighLowPrices() throws IOException {
        highLowForPeriodService.saveHighLowPricesForPeriod(StockPerformanceInterval.STOCK_PERF_INTERVAL_52W);
    }

    @PostMapping("/save-30d-high-low")
    @ResponseStatus(HttpStatus.OK)
    public void save30DayHighLowPrices() throws IOException {
        highLowForPeriodService.saveHighLowPricesForPeriod(StockPerformanceInterval.STOCK_PERF_INTERVAL_30D);
    }

}
