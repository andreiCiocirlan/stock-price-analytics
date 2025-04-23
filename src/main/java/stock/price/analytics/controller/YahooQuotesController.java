package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.service.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/yahoo-quotes")
public class YahooQuotesController {

    private final YahooQuoteService yahooQuoteService;
    private final PriceService priceService;
    private final HighLowForPeriodService highLowForPeriodService;
    private final StockService stockService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final PriceMilestoneService priceMilestoneService;

    @GetMapping("/import")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> yahooQuotesImport() {
        long start = System.nanoTime();
        List<DailyPrice> dailyImportedPrices = logTimeAndReturn(yahooQuoteService::yahooQuotesImport, "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            logTime(() -> priceService.updateAllTimeframePrices(dailyImportedPrices), "updated prices for all timeframes");

            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices), "saved current week HighLow prices");
            logTime(stockService::updateStocksHighLowsAndOHLCFrom, "updated stocks highs-lows 4w,52w,all-time and higher-timeframe OHLC prices");
        }
        webSocketNotificationService.broadcastStockChartUpdate();
        logTime(priceMilestoneService::cacheTickersForMilestones, "cached tickers for price milestones");
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Real-time import done in {} ms", duration);
        return dailyImportedPrices;
    }

    @GetMapping("/from-file")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> yahooQuotesImportFromFile(@RequestParam(value = "tradingDate", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate tradingDate) {
        long start = System.nanoTime();
        LocalDate date = Objects.requireNonNullElseGet(tradingDate, stockService::findLastUpdate);
        String fileName = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        List<DailyPrice> dailyImportedPrices = logTimeAndReturn(() -> yahooQuoteService.yahooQuotesFromFile(fileName), "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            logTime(() -> priceService.updateAllTimeframePrices(dailyImportedPrices), "updated prices for all timeframes");
            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices), "saved current week HighLow prices");
            // update stocks only if the most recent trading date was imported
            if (date.isEqual(dailyImportedPrices.getFirst().getDate())) {
                logTime(stockService::updateStocksHighLowsAndOHLCFrom, "updated stocks highs-lows 4w,52w,all-time and higher-timeframe OHLC prices");
            }
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Import from file done in {} ms", duration);
        return dailyImportedPrices;
    }


}