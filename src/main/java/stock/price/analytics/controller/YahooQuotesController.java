package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.service.HighLowForPeriodService;
import stock.price.analytics.service.PricesService;
import stock.price.analytics.service.StockService;
import stock.price.analytics.service.YahooQuoteService;

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
    private final PricesService pricesService;
    private final HighLowForPeriodService highLowForPeriodService;
    private final StockService stockService;

    @Transactional
    @GetMapping("/import")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> yahooQuotesImport() {
        long start = System.nanoTime();
        List<DailyPrice> dailyImportedPrices = logTimeAndReturn(yahooQuoteService::yahooQuotesImport, "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            List<AbstractPrice> htfPricesUpdated = pricesService.updatePricesForHigherTimeframes(dailyImportedPrices);

            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices), "saved current week HighLow prices");
            logTime(() -> stockService.updateStocksHighLowsAndOHLCFrom(dailyImportedPrices, htfPricesUpdated), "updated stocks highs-lows 4w,52w,all-time and higher-timeframe OHLC prices");
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Real-time import done in {} ms", duration);
        return dailyImportedPrices;
    }

    @Transactional
    @GetMapping("/from-file")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> yahooQuotesImportFromFile(@RequestParam(value = "fileName", required = false) String fileNameStr) {
        long start = System.nanoTime();
        String fileName = Objects.requireNonNullElseGet(fileNameStr, () -> tradingDateNow().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "_1");
        List<DailyPrice> dailyImportedPrices = logTimeAndReturn(() -> yahooQuoteService.yahooQuotesFromFile(fileName), "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            pricesService.savePrices(dailyImportedPrices);
            List<AbstractPrice> htfPricesUpdated = pricesService.updatePricesForHigherTimeframes(dailyImportedPrices);

            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices), "saved current week HighLow prices");
            logTime(() -> stockService.updateStocksHighLowsAndOHLCFrom(dailyImportedPrices, htfPricesUpdated), "updated stocks highs-lows 4w,52w,all-time and higher-timeframe OHLC prices");
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Import from file done in {} ms", duration);
        return dailyImportedPrices;
    }


}