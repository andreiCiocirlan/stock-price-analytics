package com.example.stockprices.controller;

import com.example.stockprices.model.prices.enums.StockTimeframe;
import com.example.stockprices.service.StockHistoPricesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StockHistoPricesController {

    private final StockHistoPricesService stockHistoPricesService;

    @PostMapping("/last_week_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveLastWeekPricesFromFiles() {
        stockHistoPricesService.saveLastWeekPricesFromFiles();
    }

    @PostMapping("/daily_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveDailyPrices() {
        stockHistoPricesService.saveDailyPricesFromFiles();
    }

    @PostMapping("/weekly_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveWeeklyPrices() {
        stockHistoPricesService.savePricesFromFileAndTimeframe(StockTimeframe.WEEKLY);
    }

    @PostMapping("/monthly_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveMonthlyPrices() {
        stockHistoPricesService.savePricesFromFileAndTimeframe(StockTimeframe.MONTHLY);
    }

    @PostMapping("/yearly_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveYearlyPrices() {
        stockHistoPricesService.savePricesFromFileAndTimeframe(StockTimeframe.YEARLY);
    }

}
