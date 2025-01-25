package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockDiscrepanciesRepository;

import java.util.List;

@RequestMapping("/discrepancies")
@RestController
@RequiredArgsConstructor
public class StockDiscrepanciesController {

    private final StockDiscrepanciesRepository stockDiscrepanciesRepository;

    @GetMapping("/find-any")
    public List<Object[]> findStocksHighLowsOrHTFDiscrepancies() {
        return stockDiscrepanciesRepository.findStocksHighLowsOrHTFDiscrepancies();
    }

    @GetMapping("/high-low-for-period")
    public List<Stock> findStocksWithHighLowDiscrepancy(@RequestParam(value = "period") HighLowPeriod period) {
        return switch (period) {
            case HIGH_LOW_4W -> stockDiscrepanciesRepository.findStocksWithHighLow4wDiscrepancy();
            case HIGH_LOW_52W -> stockDiscrepanciesRepository.findStocksWithHighLow52wDiscrepancy();
            case HIGH_LOW_ALL_TIME -> stockDiscrepanciesRepository.findStocksWithHighestLowestDiscrepancy();
        };
    }

    @GetMapping("/weekly-opening")
    public List<Stock> findStocksWithWeeklyOpeningDiscrepancy() {
        return stockDiscrepanciesRepository.findStocksWithWeeklyOpeningDiscrepancy();
    }

    @GetMapping("/monthly-opening")
    public List<Stock> findStocksWithMonthlyOpeningDiscrepancy() {
        return stockDiscrepanciesRepository.findStocksWithMonthlyOpeningDiscrepancy();
    }

    @GetMapping("/quarterly-opening")
    public List<Stock> findStocksWithQuarterlyOpeningDiscrepancy() {
        return stockDiscrepanciesRepository.findStocksWithQuarterlyOpeningDiscrepancy();
    }

    @GetMapping("/yearly-opening")
    public List<Stock> findStocksWithYearlyOpeningDiscrepancy() {
        return stockDiscrepanciesRepository.findStocksWithYearlyOpeningDiscrepancy();
    }

    @PutMapping("/weekly-opening")
    public void updateStocksWithWeeklyOpeningDiscrepancy() {
        stockDiscrepanciesRepository.updateStocksWithWeeklyOpeningDiscrepancy();
    }

    @PutMapping("/monthly-opening")
    public void updateStocksWithMonthlyOpeningDiscrepancy() {
        stockDiscrepanciesRepository.updateStocksWithMonthlyOpeningDiscrepancy();
    }

    @PutMapping("/quarterly-opening")
    public void updateStocksWithQuarterlyOpeningDiscrepancy() {
        stockDiscrepanciesRepository.updateStocksWithQuarterlyOpeningDiscrepancy();
    }

    @PutMapping("/yearly-opening")
    public void updateStocksWithYearlyOpeningDiscrepancy() {
        stockDiscrepanciesRepository.updateStocksWithYearlyOpeningDiscrepancy();
    }

}