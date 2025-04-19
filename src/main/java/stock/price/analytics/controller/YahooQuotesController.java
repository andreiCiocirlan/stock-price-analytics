package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.service.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;


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
            List<AbstractPrice> htfPricesUpdated = priceService.updatePricesForHigherTimeframes(dailyImportedPrices);

            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices), "saved current week HighLow prices");
            logTime(() -> stockService.updateStocksHighLowsAndOHLCFrom(dailyImportedPrices, htfPricesUpdated), "updated stocks highs-lows 4w,52w,all-time and higher-timeframe OHLC prices");
        }
        webSocketNotificationService.broadcastStockChartUpdate();
        logTime(priceMilestoneService::cacheTickersForMilestones, "cached tickers for price milestones");
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Real-time import done in {} ms", duration);
        return dailyImportedPrices;
    }

    @GetMapping("/from-file")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> yahooQuotesImportFromFile(@RequestParam(value = "fileName", required = false) String fileNameStr) {
        long start = System.nanoTime();
        String fileName = Objects.requireNonNullElseGet(fileNameStr, () -> tradingDateNow().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        List<DailyPrice> dailyImportedPrices = logTimeAndReturn(() -> yahooQuoteService.yahooQuotesFromFile(fileName), "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            priceService.savePrices(dailyImportedPrices);
            List<AbstractPrice> htfPricesUpdated = priceService.updatePricesForHigherTimeframes(dailyImportedPrices);

            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices), "saved current week HighLow prices");
            // update stocks only if the most recent trading date was imported
            if (tradingDateNow().isEqual(dailyImportedPrices.getFirst().getDate())) {
                logTime(() -> stockService.updateStocksHighLowsAndOHLCFrom(dailyImportedPrices, htfPricesUpdated), "updated stocks highs-lows 4w,52w,all-time and higher-timeframe OHLC prices");
            }
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Import from file done in {} ms", duration);
        return dailyImportedPrices;
    }


}