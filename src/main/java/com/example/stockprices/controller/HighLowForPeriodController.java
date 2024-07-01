package com.example.stockprices.controller;

import com.example.stockprices.model.prices.enums.StockPerformanceInterval;
import com.example.stockprices.service.HighLowForPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

import static java.time.LocalDate.of;

@RestController
@RequiredArgsConstructor
public class HighLowForPeriodController {

    public static final LocalDate START_DATE = of(1960, 1, 10);
    public static final LocalDate END_DATE = of(2000, 1, 9);
    private final HighLowForPeriodService highLowForPeriodService;

    @PostMapping("/save-52w-high-low")
    @ResponseStatus(HttpStatus.OK)
    public void save52WeekHighLowPrices() throws IOException {
        highLowForPeriodService.saveHighLowPricesForPeriod(START_DATE, END_DATE, StockPerformanceInterval.STOCK_PERF_INTERVAL_52W);
    }

    @PostMapping("/save-30d-high-low")
    @ResponseStatus(HttpStatus.OK)
    public void save30DayHighLowPrices() throws IOException {
        highLowForPeriodService.saveHighLowPricesForPeriod(START_DATE, END_DATE, StockPerformanceInterval.STOCK_PERF_INTERVAL_30D);
    }

}
