package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.StockHistoricalPricesService;

import java.time.LocalDate;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StockHistoricalPricesController {

    private final StockHistoricalPricesService stockHistoricalPricesService;

    @PostMapping("/prices_for_trading_date")
    @ResponseStatus(HttpStatus.OK)
    public void saveAllPricesForTradingDate(@RequestParam(required = false, value = "tickers") String tickers,
                                          @RequestParam(value = "prevDaysCount") int prevDaysCount,
                                          @RequestParam(name = "tradingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradingDate) {
        stockHistoricalPricesService.savePricesForTradingDate(tickers, prevDaysCount, tradingDate);
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
