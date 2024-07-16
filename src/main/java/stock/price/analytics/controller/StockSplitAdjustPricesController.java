package stock.price.analytics.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPriceOHLC;
import stock.price.analytics.service.StockSplitAdjustPricesService;

import java.time.LocalDate;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/stock-split")
class StockSplitAdjustPricesController {

    private final StockSplitAdjustPricesService stockSplitAdjustPricesService;

    @PostMapping("/adjust-prices")
    void splitAdjustPrices(@RequestParam("ticker") String ticker,
                           @RequestParam("stockSplitDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate stockSplitDate,
                           @RequestParam("priceMultiplier") double priceMultiplier) {
        stockSplitAdjustPricesService.adjustPricesFor(ticker, stockSplitDate, priceMultiplier);
    }

    @PostMapping("/adjust-prices-for-date")
    List<? extends AbstractPriceOHLC> splitAdjustPricesForDateAndTimeframe(@RequestParam("ticker") String ticker,
                                              @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                              @RequestParam("priceMultiplier") double priceMultiplier,
                                              @RequestParam("timeframe") StockTimeframe timeframe,
                                              @RequestParam("ohlc") @Valid @Pattern(regexp = "^[OHLCohlc]{1,4}$", message = "Invalid OHLC") String ohlc) {
        return stockSplitAdjustPricesService.adjustPricesForDateAndTimeframe(ticker, date, priceMultiplier, timeframe, ohlc);
    }


}
