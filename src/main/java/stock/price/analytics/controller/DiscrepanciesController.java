package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.service.DiscrepancieService;

import java.util.List;

@RequestMapping("/discrepancies")
@RestController
@RequiredArgsConstructor
public class DiscrepanciesController {

    private final DiscrepancieService discrepancieService;

    @GetMapping("/stocks/find-all")
    public List<String> findAllStockDiscrepancies() {
        return discrepancieService.findAllStocksDiscrepancies();
    }

    @GetMapping("/weekly-prices/find-all")
    public List<String> findAllWeeklyPriceDiscrepancies() {
        return discrepancieService.findAllWeeklyPriceDiscrepancies();
    }

    @PutMapping("/stocks/high-low-for-period")
    public void updateStocksWithHighLowDiscrepancy(@RequestParam(value = "period") HighLowPeriod period) {
        discrepancieService.updateStocksWithHighLowDiscrepancyFor(period);
    }

    @PutMapping("/weekly-prices/opening-prices")
    public void updateHTFPricesWithOpeningPriceDiscrepancy(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        discrepancieService.updateHTFOpeningPricesDiscrepancyFor(timeframe);
    }

    @GetMapping("/stocks/opening-price")
    public List<String> findStocksWithOpeningPriceDiscrepancy(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return discrepancieService.findStocksWithOpeningPriceDiscrepancyFor(timeframe);
    }

    @PutMapping("/stocks/opening-price")
    public void updateStocksWithOpeningPriceDiscrepancyFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        discrepancieService.updateStocksWithOpeningPriceDiscrepancyFor(timeframe);
    }

}