package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.client.finnhub.FinnhubClient;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.PriceOHLCRepository;
import stock.price.analytics.service.*;

import java.time.LocalDate;
import java.util.List;

import static stock.price.analytics.util.TradingDateUtil.tradingDateImported;


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
        List<DailyPriceOHLC> dailyImportedPrices = yahooQuoteService.dailyPricesImport();
        if (!dailyImportedPrices.isEmpty()) {
            priceOHLCService.updatePricesForHigherTimeframes(dailyImportedPrices);
            stockService.updateStocksDate(dailyImportedPrices);
            // daily performance view based on stock last_updated (keep this order)
            refreshMaterializedViewsService.refreshMaterializedViews();

            // high/low price update based on weekly perf view (refreshed before)
            LocalDate tradingDate = tradingDateImported(dailyImportedPrices);
            List<String> tickers = dailyImportedPrices.stream().map(DailyPriceOHLC::getTicker).toList();
            highLowForPeriodService.saveCurrentWeekHighLowPrices(tickers, tradingDate);
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
        List<DailyPriceOHLC> dailyImportedPrices = yahooQuoteService.dailyPricesFromFile(fileName);
        if (!dailyImportedPrices.isEmpty()) {
            priceOHLCService.savePrices(dailyImportedPrices);
            priceOHLCService.updatePricesForHigherTimeframes(dailyImportedPrices);
            stockService.updateStocksDate(dailyImportedPrices);
            // daily performance view based on stock last_updated (keep this order)
            refreshMaterializedViewsService.refreshMaterializedViews();

            // high/low price update based on weekly perf view (refreshed before)
            LocalDate tradingDate = tradingDateImported(dailyImportedPrices);
            List<String> tickers = dailyImportedPrices.stream().map(DailyPriceOHLC::getTicker).toList();
            highLowForPeriodService.saveCurrentWeekHighLowPrices(tickers, tradingDate);
        }
        return dailyImportedPrices;
    }

}