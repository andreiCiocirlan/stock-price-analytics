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
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.service.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/stock-split")
public class StockSplitAdjustPricesController {

    private final StockSplitAdjustPricesService stockSplitAdjustPricesService;
    private final PricesService pricesService;
    private final HighLowForPeriodService highLowForPeriodService;
    private final FairValueGapService fairValueGapService;
    private final StockService stockService;

    @PostMapping("/adjust-prices")
    void splitAdjustPrices(@RequestParam("ticker") String ticker,
                           @RequestParam("stockSplitDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate stockSplitDate,
                           @RequestParam("priceMultiplier") double priceMultiplier) {
        stockSplitAdjustPricesService.adjustPricesFor(ticker, stockSplitDate, priceMultiplier);
        pricesService.updateHtfPricesPerformanceFor(stockSplitDate, ticker);
        highLowForPeriodService.saveAllHistoricalHighLowPrices(List.of(ticker), stockSplitDate);
        fairValueGapService.updateFVGPricesForStockSplit(ticker, stockSplitDate, priceMultiplier);

        // stockSplitDate within the last_updated week
        if (stockSplitDate.isAfter(tradingDateNow().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))) {
            stockService.updateStockHigherTimeframePricesFor(ticker);
            stockService.updateHighLowForPeriodPrices(ticker);
            if (stockSplitDate.isEqual(tradingDateNow())) {
                stockService.updateStockDailyPricesFor(ticker);
            }
        }
    }

    @PostMapping("/adjust-prices-for-date")
    List<? extends AbstractPrice> splitAdjustPricesForDateAndTimeframe(@RequestParam("ticker") String ticker,
                                                                       @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                                       @RequestParam("priceMultiplier") double priceMultiplier,
                                                                       @RequestParam("timeframe") StockTimeframe timeframe,
                                                                       @RequestParam("ohlc") @Valid @Pattern(regexp = "^[OHLCohlc]{1,4}$", message = "Invalid OHLC") String ohlc) {
        return stockSplitAdjustPricesService.adjustPricesForDateAndTimeframe(ticker, date, priceMultiplier, timeframe, ohlc);
    }

}
