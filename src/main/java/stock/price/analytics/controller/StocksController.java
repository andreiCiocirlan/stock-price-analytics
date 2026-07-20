package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.StockService;

import java.time.LocalDate;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/stock")
public class StocksController {

    private final StockService stockService;

    @PostMapping("/split-adjust-prices")
    void splitAdjustPrices(@RequestParam("ticker") String ticker,
                           @RequestParam("stockSplitDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate stockSplitDate,
                           @RequestParam("priceMultiplier") double priceMultiplier) {
        stockService.splitAdjustFor(ticker, stockSplitDate, priceMultiplier);
    }

    @GetMapping("/log-most-extended-above-200sma")
    void logExtendedAbove200SMA(@RequestParam(value = "timeframe") StockTimeframe timeframe,
                                @RequestParam("tradingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradingDate,
                                @RequestParam(value = "cfdMargin") Double cfdMargin) {
        stockService.logExtendedAbove200SMA(timeframe, tradingDate, cfdMargin);
    }

}
