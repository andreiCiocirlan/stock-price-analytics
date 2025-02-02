package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
@RequestMapping("/stock-prices")
public class IntraDayPricesController {

    private final YahooQuoteService yahooQuoteService;
    private final PricesService pricesService;
    private final HighLowForPeriodService highLowForPeriodService;
    private final StockService stockService;

    @Transactional
    @GetMapping("/yahoo-prices/intraday")
    public void yahooPricesImport() {
        long start = System.nanoTime();
        List<DailyPrice> dailyImportedPrices = logTimeAndReturn(yahooQuoteService::dailyPricesImport, "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            List<AbstractPrice> htfPricesUpdated = pricesService.updatePricesForHigherTimeframes(dailyImportedPrices);

            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices), "saved current week HighLow prices" );
            logTime(() -> stockService.updateStocksHighLowsAndOHLCFrom(dailyImportedPrices, htfPricesUpdated), "updated stocks highs-lows 4w,52w,all-time and higher-timeframe OHLC prices");
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Real-time import done in {} ms", duration);
    }

    @Transactional
    @GetMapping("/yahoo-prices/from-file")
    public List<DailyPrice> yahooPricesImportFromFile(@RequestParam(value = "fileName", required = false) String fileNameStr) {
        long start = System.nanoTime();
        String fileName = Objects.requireNonNullElseGet(fileNameStr, () -> tradingDateNow().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "_1");
        List<DailyPrice> dailyImportedPrices = logTimeAndReturn(() -> yahooQuoteService.dailyPricesFromFile(fileName), "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            pricesService.savePrices(dailyImportedPrices);
            List<AbstractPrice> htfPricesUpdated = pricesService.updatePricesForHigherTimeframes(dailyImportedPrices);

            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices), "saved current week HighLow prices" );
            logTime(() -> stockService.updateStocksHighLowsAndOHLCFrom(dailyImportedPrices, htfPricesUpdated), "updated stocks highs-lows 4w,52w,all-time and higher-timeframe OHLC prices");
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Import from file done in {} ms", duration);
        return dailyImportedPrices;
    }


}