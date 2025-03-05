package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockDiscrepanciesRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDiscrepanciesService {

    private final StockDiscrepanciesRepository stockDiscrepanciesRepository;

    public List<String> findAllStockDiscrepancies() {
        List<String> stocksWithDiscrepancies = findStocksHighLowsOrHTFDiscrepancies();
        stocksWithDiscrepancies.addAll(findHighLowAndOpeningPriceDiscrepancies());
        return stocksWithDiscrepancies;
    }

    private List<String> findStocksHighLowsOrHTFDiscrepancies() {
        List<String> stocksWithDiscrepancies = new ArrayList<>();
        List<Object[]> stocksHighLowsOrHTFDiscrepancies = stockDiscrepanciesRepository.findStocksHighLowsOrHTFDiscrepancies();
        if (!stocksHighLowsOrHTFDiscrepancies.isEmpty()) {
            stocksHighLowsOrHTFDiscrepancies.forEach(resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), stocksWithDiscrepancies));
        }
        return stocksWithDiscrepancies;
    }

    private List<String> findHighLowAndOpeningPriceDiscrepancies() {
        List<String> stocksWithDiscrepancies = new ArrayList<>();
        Map<String, Supplier<List<Stock>>> discrepancyMethods = Map.of(
                "52-week High/Low", stockDiscrepanciesRepository::findStocksWithHighLow52wDiscrepancy,
                "4-week High/Low", stockDiscrepanciesRepository::findStocksWithHighLow4wDiscrepancy,
                "Highest Lowest", stockDiscrepanciesRepository::findStocksWithHighestLowestDiscrepancy,
                "Weekly Opening", stockDiscrepanciesRepository::findStocksWithWeeklyOpeningDiscrepancy,
                "Monthly Opening", stockDiscrepanciesRepository::findStocksWithMonthlyOpeningDiscrepancy,
                "Quarterly Opening", stockDiscrepanciesRepository::findStocksWithQuarterlyOpeningDiscrepancy,
                "Yearly Opening", stockDiscrepanciesRepository::findStocksWithYearlyOpeningDiscrepancy
        );

        discrepancyMethods.forEach((discrepancyType, stockDiscrepancies) ->
                stockDiscrepancies.get().forEach(stock -> logDiscrepancyAndAddToList(stock.getTicker(), discrepancyType, stocksWithDiscrepancies)));

        return stocksWithDiscrepancies;
    }

    private void logDiscrepancyAndAddToList(String ticker, String discrepancyType, List<String> discrepanciesFound) {
        String stockDiscrepancy = String.format("%s with %s discrepancy", ticker, discrepancyType);
        discrepanciesFound.add(stockDiscrepancy);
        log.warn("{}", stockDiscrepancy);
    }

}