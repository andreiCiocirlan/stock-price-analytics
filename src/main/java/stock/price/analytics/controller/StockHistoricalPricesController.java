package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.StockHistoricalPricesService;
import stock.price.analytics.util.FileUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class StockHistoricalPricesController {

    private final StockHistoricalPricesService stockHistoricalPricesService;


    @PostMapping("/prices_for_trading_date")
    @ResponseStatus(HttpStatus.OK)
    public void savePricesForTradingDate(@RequestParam(required = false, value = "tickers") String tickers,
                                         @RequestParam(name = "tradingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradingDate,
                                         @RequestParam(name = "higherTimeFrameDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate higherTimeFrameDate) throws IOException {
        List<String> tickerList = tickers != null ? Arrays.stream(tickers.split(",")).toList() : FileUtils.readTickersXTB();
        stockHistoricalPricesService.savePricesForTradingDate(tickerList, tradingDate, higherTimeFrameDate);
    }

    @PostMapping("/prices_after_trading_date")
    @ResponseStatus(HttpStatus.OK)
    public void savePricesAfterTradingDate(@RequestParam(required = false, value = "tickers") String tickers,
                                           @RequestParam(value = "prevDaysCount") int prevDaysCount,
                                           @RequestParam(name = "tradingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradingDate) {
        stockHistoricalPricesService.savePricesAfterTradingDate(tickers, prevDaysCount, tradingDate);
    }

    @PostMapping("/daily_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveAllDailyPrices() {
        stockHistoricalPricesService.saveAllDailyPricesFromFiles();
    }

    @PostMapping("/weekly_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveWeeklyPrices() {
        stockHistoricalPricesService.savePricesForTimeframe(StockTimeframe.WEEKLY);
    }

    @PostMapping("/monthly_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveMonthlyPrices() {
        stockHistoricalPricesService.savePricesForTimeframe(StockTimeframe.MONTHLY);
    }

    @PostMapping("/yearly_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveYearlyPrices() {
        stockHistoricalPricesService.savePricesForTimeframe(StockTimeframe.YEARLY);
    }

}
