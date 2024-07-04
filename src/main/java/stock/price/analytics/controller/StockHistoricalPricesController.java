package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.StockHistoricalPricesService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StockHistoricalPricesController {

    private final StockHistoricalPricesService stockHistoricalPricesService;

    @PostMapping("/last_week_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveLastWeekPricesFromFiles() {
        stockHistoricalPricesService.saveLastWeekPricesFromFiles();
    }

    @PostMapping("/daily_prices")
    @ResponseStatus(HttpStatus.OK)
    public void saveDailyPrices() {
        stockHistoricalPricesService.saveDailyPricesFromFiles();
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
