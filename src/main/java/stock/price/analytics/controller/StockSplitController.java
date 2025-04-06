package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.service.StockSplitService;

import java.time.LocalDate;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/stock-split")
public class StockSplitController {

    private final StockSplitService stockSplitService;

    @PostMapping("/adjust-prices")
    void splitAdjustPrices(@RequestParam("ticker") String ticker,
                           @RequestParam("stockSplitDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate stockSplitDate,
                           @RequestParam("priceMultiplier") double priceMultiplier) {
        stockSplitService.splitAdjustFor(ticker, stockSplitDate, priceMultiplier);
    }

}
