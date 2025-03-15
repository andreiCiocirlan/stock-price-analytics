package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.service.DiscrepanciesService;

import java.util.List;

@RequestMapping("/discrepancies")
@RestController
@RequiredArgsConstructor
public class DiscrepanciesController {

    private final DiscrepanciesService discrepanciesService;

    @GetMapping("/stocks/find-all")
    public List<String> findAllStockDiscrepancies() {
        return discrepanciesService.findAllStocksDiscrepancies();
    }

    @GetMapping("/weekly-prices/find-all")
    public List<String> findAllWeeklyPriceDiscrepancies() {
        return discrepanciesService.findAllWeeklyPriceDiscrepancies();
    }

    @PutMapping("/stocks/high-low-for-period")
    public void updateStocksWithHighLowDiscrepancy(@RequestParam(value = "period") HighLowPeriod period) {
        discrepanciesService.updateStocksWithHighLowDiscrepancyFor(period);
    }

    @PutMapping("/weekly-prices/opening-prices")
    public void updateHTFPricesWithOpeningPriceDiscrepancy(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        discrepanciesService.updateHTFOpeningPricesDiscrepancyFor(timeframe);
    }

    @GetMapping("/stocks/opening-price")
    public List<Stock> findStocksWithOpeningPriceDiscrepancy(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return discrepanciesService.findStocksWithOpeningPriceDiscrepancyFor(timeframe);
    }

    @PutMapping("/stocks/opening-price")
    public void updateStocksWithOpeningPriceDiscrepancyFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        discrepanciesService.updateStocksWithOpeningPriceDiscrepancyFor(timeframe);
    }

}