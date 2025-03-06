package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockDiscrepanciesRepository;
import stock.price.analytics.service.StockDiscrepanciesService;

import java.util.List;

@RequestMapping("/discrepancies")
@RestController
@RequiredArgsConstructor
public class StockDiscrepanciesController {

    private final StockDiscrepanciesRepository stockDiscrepanciesRepository;
    private final StockDiscrepanciesService stockDiscrepanciesService;

    @GetMapping("/find-all")
    public List<String> findAllDiscrepancies() {
        return stockDiscrepanciesService.findAllStockDiscrepancies();
    }

    @PutMapping("/high-low-for-period")
    public void updateStocksWithHighLowDiscrepancy(@RequestParam(value = "period") HighLowPeriod period) {
        switch (period) {
            case HIGH_LOW_4W -> stockDiscrepanciesRepository.updateStocksWithHighLow4wDiscrepancy();
            case HIGH_LOW_52W -> stockDiscrepanciesRepository.updateStocksWithHighLow52wDiscrepancy();
            case HIGH_LOW_ALL_TIME -> stockDiscrepanciesRepository.updateStocksWithHighestLowestDiscrepancy();
        }
    }

    @GetMapping("/opening-price")
    public List<Stock> findStocksWithOpeningPriceDiscrepancy(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> stockDiscrepanciesRepository.findStocksWithWeeklyOpeningDiscrepancy();
            case MONTHLY -> stockDiscrepanciesRepository.findStocksWithMonthlyOpeningDiscrepancy();
            case QUARTERLY -> stockDiscrepanciesRepository.findStocksWithQuarterlyOpeningDiscrepancy();
            case YEARLY -> stockDiscrepanciesRepository.findStocksWithYearlyOpeningDiscrepancy();
        };
    }

    @PutMapping("/opening-price")
    public void updateStocksWithOpeningPriceDiscrepancy(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> stockDiscrepanciesRepository.updateStocksWithWeeklyOpeningDiscrepancy();
            case MONTHLY -> stockDiscrepanciesRepository.updateStocksWithMonthlyOpeningDiscrepancy();
            case QUARTERLY -> stockDiscrepanciesRepository.updateStocksWithQuarterlyOpeningDiscrepancy();
            case YEARLY -> stockDiscrepanciesRepository.updateStocksWithYearlyOpeningDiscrepancy();
        }
    }

}