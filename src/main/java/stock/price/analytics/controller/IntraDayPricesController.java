package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.client.finnhub.FinnhubClient;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.PriceOHLCRepository;
import stock.price.analytics.service.*;

import java.util.List;

import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;


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
    private final DailyPricesJSONService dailyPricesJSONService;

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
        List<DailyPriceOHLC> dailyImportedPrices = logTimeAndReturn(yahooQuoteService::dailyPricesImport, "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            priceOHLCService.updatePricesForHigherTimeframes(dailyImportedPrices);
            logTime(() -> stockService.updateStocksOHLCFrom(dailyImportedPrices), "updated stocks OHLC, last_updated, and performance from daily imported prices ");
            // daily performance view based on stock last_updated (keep this order)
            refreshMaterializedViewsService.refreshMaterializedViews();

            // high/low price update based on weekly perf view (refreshed before)
            List<String> tickers = dailyImportedPrices.stream().map(DailyPriceOHLC::getTicker).toList();
            highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices, tickers);
            logTime(stockService::updateStocksHighLowFromHighLowCache, "updated stocks high low 4w, 52w, all-time");
        }
    }

    @Transactional
    @GetMapping("/yahoo-prices/pre-market")
    public void yahooPreMarketPricesImport() {
        yahooQuoteService.dailyPricesImport(true);
    }

    @Transactional
    @GetMapping("/yahoo-prices/from-file")
    public List<DailyPriceOHLC> yahooPricesImportFromFile(@RequestParam("fileName") String fileName) {
        List<DailyPriceOHLC> dailyImportedPrices = logTimeAndReturn(() -> yahooQuoteService.dailyPricesFromFile(fileName), "imported daily prices");
        if (dailyImportedPrices != null && !dailyImportedPrices.isEmpty()) {
            priceOHLCService.savePrices(dailyImportedPrices);
            priceOHLCService.updatePricesForHigherTimeframes(dailyImportedPrices);
            logTime(() -> stockService.updateStocksOHLCFrom(dailyImportedPrices), "updated stocks OHLC, last_updated, and performance from daily imported prices ");
            // daily performance view based on stock last_updated (keep this order)
            refreshMaterializedViewsService.refreshMaterializedViews();

            // high/low price update based on weekly perf view (refreshed before)
            List<String> tickers = dailyImportedPrices.stream().map(DailyPriceOHLC::getTicker).toList();
            highLowForPeriodService.saveCurrentWeekHighLowPricesFrom(dailyImportedPrices, tickers);
            logTime(stockService::updateStocksHighLowFromHighLowCache, "updated stocks high low 4w, 52w, all-time");
        }
        return dailyImportedPrices;
    }

    @Transactional
    @PostMapping("/yahoo-prices/save-json-from-file")
    public void saveDailyPricesJSONFrom(@RequestParam("fileName") String fileName) {
        dailyPricesJSONService.saveDailyPricesJSONFrom(fileName);
    }

}