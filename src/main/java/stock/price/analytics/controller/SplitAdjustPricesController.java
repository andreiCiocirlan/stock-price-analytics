package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.service.SplitAdjustPricesService;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stock-split")
class SplitAdjustPricesController {

    private final SplitAdjustPricesService splitAdjustPricesService;

    @PostMapping("/adjust-prices")
    void splitAdjustPrices(@RequestParam("ticker") String ticker,
                           @RequestParam("stockSplitDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate stockSplitDate,
                           @RequestParam("priceMultiplier") double priceMultiplier) {
        splitAdjustPricesService.adjustPricesFor(ticker, stockSplitDate, priceMultiplier);
    }
}
