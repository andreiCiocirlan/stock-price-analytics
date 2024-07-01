package com.example.stockprices.controller;

import com.example.stockprices.service.StockHistoricalPricesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StockHistoricalPricesController {

    private final StockHistoricalPricesService stockHistoricalPricesService;

    @PostMapping("/save_last_week")
    @ResponseStatus(HttpStatus.OK)
    public void saveLastWeekPrices() {
        stockHistoricalPricesService.saveLastWeekPricesFromFiles();
    }

}