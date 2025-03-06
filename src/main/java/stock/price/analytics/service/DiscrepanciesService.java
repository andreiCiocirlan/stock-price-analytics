package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.enums.StockTimeframe;
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

    public List<String> findAllStocksDiscrepancies() {
        List<String> stocksWithDiscrepancies = findStocksHighLowsOrHTFDiscrepancies();
        stocksWithDiscrepancies.addAll(findStocksOpeningPriceDiscrepancies());
        return stocksWithDiscrepancies;
    }

    public List<String> findAllWeeklyPriceDiscrepancies() {
        List<String> weeklyPriceDiscrepancies = findWeeklyOpeningPriceDiscrepancies();
        weeklyPriceDiscrepancies.addAll(findWeeklyHighLowPriceDiscrepancies());
        return weeklyPriceDiscrepancies;
    }

    public List<String> findStocksHighLowsOrHTFDiscrepancies() {
        List<String> stocksWithDiscrepancies = new ArrayList<>();
        List<Object[]> stocksHighLowsOrHTFDiscrepancies = stockDiscrepanciesRepository.findStocksHighLowsOrHTFDiscrepancies();
        if (!stocksHighLowsOrHTFDiscrepancies.isEmpty()) {
            stocksHighLowsOrHTFDiscrepancies.forEach(resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), stocksWithDiscrepancies));
        }
        return stocksWithDiscrepancies;
    }

    public List<String> findStocksOpeningPriceDiscrepancies() {
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

    public List<Stock> findStocksWithOpeningPriceDiscrepancyFor(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> stockDiscrepanciesRepository.findStocksWithWeeklyOpeningDiscrepancy();
            case MONTHLY -> stockDiscrepanciesRepository.findStocksWithMonthlyOpeningDiscrepancy();
            case QUARTERLY -> stockDiscrepanciesRepository.findStocksWithQuarterlyOpeningDiscrepancy();
            case YEARLY -> stockDiscrepanciesRepository.findStocksWithYearlyOpeningDiscrepancy();
        };
    }

    public void updateWeeklyPricesWithOpeningPriceDiscrepancy() {
        pricesDiscrepanciesRepository.updateWeeklyPricesWithOpeningPriceDiscrepancy();
    }

    public void updateStocksWithOpeningPriceDiscrepancyFor(StockTimeframe timeframe) {
        switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> stockDiscrepanciesRepository.updateStocksWithWeeklyOpeningDiscrepancy();
            case MONTHLY -> stockDiscrepanciesRepository.updateStocksWithMonthlyOpeningDiscrepancy();
            case QUARTERLY -> stockDiscrepanciesRepository.updateStocksWithQuarterlyOpeningDiscrepancy();
            case YEARLY -> stockDiscrepanciesRepository.updateStocksWithYearlyOpeningDiscrepancy();
        }
    }

    public void updateStocksWithHighLow4wDiscrepancyFor(HighLowPeriod period) {
        switch (period) {
            case HIGH_LOW_4W -> updateStocksWithHighLow4wDiscrepancy();
            case HIGH_LOW_52W -> updateStocksWithHighLow52wDiscrepancy();
            case HIGH_LOW_ALL_TIME -> updateStocksWithHighestLowestDiscrepancy();
        }
    }

    private void updateStocksWithHighLow4wDiscrepancy() {
        stockDiscrepanciesRepository.updateStocksWithHighLow4wDiscrepancy();
    }

    private void updateStocksWithHighLow52wDiscrepancy() {
        stockDiscrepanciesRepository.updateStocksWithHighLow52wDiscrepancy();
    }

    private void updateStocksWithHighestLowestDiscrepancy() {
        stockDiscrepanciesRepository.updateStocksWithHighestLowestDiscrepancy();
    }

    private void logDiscrepancyAndAddToList(String ticker, String discrepancyType, List<String> discrepanciesFound) {
        String discrepancy = String.format("%s with %s discrepancy", ticker, discrepancyType);
        discrepanciesFound.add(discrepancy);
        log.warn("{}", discrepancy);
    }
}
