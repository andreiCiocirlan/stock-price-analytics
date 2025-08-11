package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.gaps.FVGRepository;
import stock.price.analytics.repository.prices.ohlc.PriceDiscrepanciesRepository;
import stock.price.analytics.repository.stocks.StockDiscrepanciesRepository;
import stock.price.analytics.util.query.pricediscrepancy.PriceDiscrepancyQueryProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscrepanciesService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final PriceDiscrepanciesRepository priceDiscrepanciesRepository;
    private final StockDiscrepanciesRepository stockDiscrepanciesRepository;
    private final FVGRepository fvgRepository;
    private final PriceDiscrepancyQueryProvider priceDiscrepancyQueryProvider;

    public List<String> findFvgDateDiscrepancies() {
        List<String> fvgDateDiscrepanciesResult = new ArrayList<>();
        List<Object[]> fvgDateDiscrepancies = fvgRepository.findFvgDateDiscrepancies();
        if (!fvgDateDiscrepancies.isEmpty()) {
            fvgDateDiscrepancies.forEach(resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), fvgDateDiscrepanciesResult));
        }
        return fvgDateDiscrepanciesResult;
    }

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
        List<Object[]> weeklyOpeningPriceDiscrepancies = priceDiscrepanciesRepository.findWeeklyOpeningPriceDiscrepancies();
        if (!weeklyOpeningPriceDiscrepancies.isEmpty()) {
            weeklyOpeningPriceDiscrepancies.forEach(
                    resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), wOpeningPriceDiscrepanciesTickers));
        }
        return wOpeningPriceDiscrepanciesTickers;
    }

    public List<String> findWeeklyHighLowPriceDiscrepancies() {
        List<String> wHighLowPriceDiscrepanciesTickers = new ArrayList<>();
        List<Object[]> weeklyHighLowPriceDiscrepancies = priceDiscrepanciesRepository.findWeeklyHighLowPriceDiscrepancies();
        if (!weeklyHighLowPriceDiscrepancies.isEmpty()) {
            weeklyHighLowPriceDiscrepancies.forEach(
                    resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), wHighLowPriceDiscrepanciesTickers));
        }
        return wHighLowPriceDiscrepanciesTickers;
    }

    public List<String> findStocksWithOpeningPriceDiscrepancyFor(StockTimeframe timeframe) {
        return (switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> stockDiscrepanciesRepository.findStocksWithWeeklyOpeningDiscrepancy();
            case MONTHLY -> stockDiscrepanciesRepository.findStocksWithMonthlyOpeningDiscrepancy();
            case QUARTERLY -> stockDiscrepanciesRepository.findStocksWithQuarterlyOpeningDiscrepancy();
            case YEARLY -> stockDiscrepanciesRepository.findStocksWithYearlyOpeningDiscrepancy();
        }).stream().map(Stock::getTicker).toList();
    }

    @Transactional
    public void updateHTFOpeningPricesDiscrepancyFor(StockTimeframe timeframe) {
        switch (timeframe) {
            case WEEKLY -> priceDiscrepanciesRepository.updateWeeklyPricesWithOpeningPriceDiscrepancy();
            case MONTHLY -> priceDiscrepanciesRepository.updateMonthlyPricesWithOpeningPriceDiscrepancy();
            case QUARTERLY -> priceDiscrepanciesRepository.updateQuarterlyPricesWithOpeningPriceDiscrepancy();
            case YEARLY -> priceDiscrepanciesRepository.updateYearlyPricesWithOpeningPriceDiscrepancy();
        }

    }

    @Transactional
    public void updateStocksWithOpeningPriceDiscrepancyFor(StockTimeframe timeframe) {
        String query = priceDiscrepancyQueryProvider.updateStocksWithOpeningPriceDiscrepancyFor(timeframe);
        int rowsAffected = entityManager.createNativeQuery(query).executeUpdate();
        log.info("updated {} rows stocks  {} opening prices", rowsAffected, timeframe);
    }

    @Transactional
    public void updateStocksWithHighLowDiscrepancyFor(HighLowPeriod period) {
        switch (period) {
            case HIGH_LOW_4W -> stockDiscrepanciesRepository.updateStocksWithHighLow4wDiscrepancy();
            case HIGH_LOW_52W -> stockDiscrepanciesRepository.updateStocksWithHighLow52wDiscrepancy();
            case HIGH_LOW_ALL_TIME -> stockDiscrepanciesRepository.updateStocksWithHighestLowestDiscrepancy();
        }
    }

    private void logDiscrepancyAndAddToList(String ticker, String discrepancyType, List<String> discrepanciesFound) {
        String discrepancy = String.format("%s with %s", ticker, discrepancyType);
        discrepanciesFound.add(discrepancy);
        log.info("{}", discrepancy);
    }
}
