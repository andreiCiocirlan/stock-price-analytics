package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.PricesDiscrepanciesRepository;
import stock.price.analytics.repository.stocks.StockDiscrepanciesRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscrepanciesService {

    private final PricesDiscrepanciesRepository pricesDiscrepanciesRepository;
    private final StockDiscrepanciesRepository stockDiscrepanciesRepository;

    public List<String> findAllStockDiscrepancies() {
        List<String> stocksWithDiscrepancies = findStocksHighLowsOrHTFDiscrepancies();
        stocksWithDiscrepancies.addAll(findStocksOpeningPriceDiscrepancies());
        return stocksWithDiscrepancies;
    }

    public List<String> findAllWeeklyPriceDiscrepancies() {
        List<String> weeklyPriceDiscrepancies = findWeeklyOpeningPriceDiscrepancies();
        weeklyPriceDiscrepancies.addAll(findWeeklyHighLowPriceDiscrepancies());
        return weeklyPriceDiscrepancies;
    }

    private List<String> findStocksHighLowsOrHTFDiscrepancies() {
        List<String> stocksWithDiscrepancies = new ArrayList<>();
        List<Object[]> stocksHighLowsOrHTFDiscrepancies = stockDiscrepanciesRepository.findStocksHighLowsOrHTFDiscrepancies();
        if (!stocksHighLowsOrHTFDiscrepancies.isEmpty()) {
            stocksHighLowsOrHTFDiscrepancies.forEach(resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), stocksWithDiscrepancies));
        }
        return stocksWithDiscrepancies;
    }

    private List<String> findStocksOpeningPriceDiscrepancies() {
        List<String> stocksWithDiscrepancies = new ArrayList<>();
        Map<String, Supplier<List<Stock>>> discrepancyMethods = Map.of(
                "Weekly Opening", stockDiscrepanciesRepository::findStocksWithWeeklyOpeningDiscrepancy,
                "Monthly Opening", stockDiscrepanciesRepository::findStocksWithMonthlyOpeningDiscrepancy,
                "Quarterly Opening", stockDiscrepanciesRepository::findStocksWithQuarterlyOpeningDiscrepancy,
                "Yearly Opening", stockDiscrepanciesRepository::findStocksWithYearlyOpeningDiscrepancy
        );

        discrepancyMethods.forEach((discrepancyType, stockDiscrepancies) ->
                stockDiscrepancies.get().forEach(stock -> logDiscrepancyAndAddToList(stock.getTicker(), discrepancyType, stocksWithDiscrepancies)));

        return stocksWithDiscrepancies;
    }

    public List<String> findWeeklyOpeningPriceDiscrepancies() {
        List<String> wOpeningPriceDiscrepanciesTickers = new ArrayList<>();
        List<Object[]> weeklyOpeningPriceDiscrepancies = pricesDiscrepanciesRepository.findWeeklyOpeningPriceDiscrepancies();
        if (!weeklyOpeningPriceDiscrepancies.isEmpty()) {
            weeklyOpeningPriceDiscrepancies.forEach(
                    resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), wOpeningPriceDiscrepanciesTickers));
        }
        return wOpeningPriceDiscrepanciesTickers;
    }

    public List<String> findWeeklyHighLowPriceDiscrepancies() {
        List<String> wHighLowPriceDiscrepanciesTickers = new ArrayList<>();
        List<Object[]> weeklyHighLowPriceDiscrepancies = pricesDiscrepanciesRepository.findWeeklyHighLowPriceDiscrepancies();
        if (!weeklyHighLowPriceDiscrepancies.isEmpty()) {
            weeklyHighLowPriceDiscrepancies.forEach(
                    resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), wHighLowPriceDiscrepanciesTickers));
        }
        return wHighLowPriceDiscrepanciesTickers;
    }

    private void logDiscrepancyAndAddToList(String ticker, String discrepancyType, List<String> discrepanciesFound) {
        String discrepancy = String.format("%s with %s discrepancy", ticker, discrepancyType);
        discrepanciesFound.add(discrepancy);
        log.warn("{}", discrepancy);
    }
}
