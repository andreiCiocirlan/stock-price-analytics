package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.client.finnhub.FinnhubClient;
import stock.price.analytics.model.prices.ohlc.AbstractPriceOHLC;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.PriceOHLCRepository;
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
@RequestMapping("/stock-prices")
public class IntraDayPricesController {

    private final YahooQuoteService yahooQuoteService;
    private final FinnhubClient finnhubClient;
    private final PriceOHLCRepository priceOHLCRepository;
    private final PriceOHLCService priceOHLCService;
    private final HighLowForPeriodService highLowForPeriodService;
    private final RefreshMaterializedViewsService refreshMaterializedViewsService;
    private final StockService stockService;

    @GetMapping("/finnhub")
    public DailyPriceOHLC intraDayPrices(@RequestParam("ticker") String ticker) {
        return finnhubClient.intraDayPricesFor(ticker).orElseThrow();
    }

    @Transactional
    @GetMapping("/finnhub-all-xtb")
    public void finnhubIntraDayPricesTickersXTB() {
        List<DailyPriceOHLC> dailyPriceOHLCs = finnhubClient.intraDayPricesXTB();
        priceOHLCRepository.saveAll(dailyPriceOHLCs);
        log.info("saved {} daily prices", dailyPriceOHLCs.size());
    }

    @Transactional
    @GetMapping("/yahoo-prices/intraday")
    public void yahooPricesImport() {
        long start = System.nanoTime();
        List<DailyPriceOHLC> dailyImportedPrices = logTimeAndReturn(yahooQuoteService::dailyPricesImport, "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            List<AbstractPriceOHLC> htfPricesUpdated = priceOHLCService.updatePricesForHigherTimeframes(dailyImportedPrices);
            logTime(() -> stockService.updateStocksOHLCFrom(dailyImportedPrices, htfPricesUpdated), "updated stocks OHLC from DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY imported prices ");
            // daily performance view based on stock last_updated (keep this order)
            logTime(refreshMaterializedViewsService::refreshMaterializedViews, "refreshed mviews");

            // high/low price update based on weekly perf view (refreshed before)
            List<String> tickers = dailyImportedPrices.stream().map(DailyPriceOHLC::getTicker).toList();
            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices, tickers), "saved current week HighLow prices" );
            logTime(stockService::updateStocksHighLowFromHighLowCache, "updated stocks high low 4w, 52w, all-time");
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Real-time import done in {} ms", duration);
    }

    @Transactional
    @GetMapping("/yahoo-prices/pre-market")
    public void yahooPreMarketPricesImport() {
        yahooQuoteService.dailyPricesImport(true);
    }

    @Transactional
    @GetMapping("/yahoo-prices/from-file")
    public List<DailyPriceOHLC> yahooPricesImportFromFile(@RequestParam(value = "fileName", required = false) String fileNameStr) {
        long start = System.nanoTime();
        String fileName = Objects.requireNonNullElseGet(fileNameStr, () -> tradingDateNow().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "_1");
        List<DailyPriceOHLC> dailyImportedPrices = logTimeAndReturn(() -> yahooQuoteService.dailyPricesFromFile(fileName), "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            priceOHLCService.savePrices(dailyImportedPrices);
            List<AbstractPriceOHLC> htfPricesUpdated = priceOHLCService.updatePricesForHigherTimeframes(dailyImportedPrices);
            logTime(() -> stockService.updateStocksOHLCFrom(dailyImportedPrices, htfPricesUpdated), "updated stocks OHLC from DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY imported prices ");
            // daily performance view based on stock last_updated (keep this order)
            logTime(refreshMaterializedViewsService::refreshMaterializedViews, "refreshed materialized views");

            // high/low price update based on weekly perf view (refreshed before)
            List<String> tickers = dailyImportedPrices.stream().map(DailyPriceOHLC::getTicker).toList();
            logTime(() -> highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices, tickers), "saved current week HighLow prices" );
            logTime(stockService::updateStocksHighLowFromHighLowCache, "updated stocks high low 4w, 52w, all-time");
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("Import from file done in {} ms", duration);
        return dailyImportedPrices;
    }


}