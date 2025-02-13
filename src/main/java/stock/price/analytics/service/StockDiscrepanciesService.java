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

    public void findStocksHighLowsOrHTFDiscrepancies() {
        List<Object[]> stocksHighLowsOrHTFDiscrepancies = stockDiscrepanciesRepository.findStocksHighLowsOrHTFDiscrepancies();
        if (!stocksHighLowsOrHTFDiscrepancies.isEmpty()) {
            log.warn("Stocks HighLowForPeriod or HTF discrepancy {}", stocksHighLowsOrHTFDiscrepancies);
        }
    }

    public void findHighLowAndOpeningPriceDiscrepancies() {
        Map<String, Supplier<List<Stock>>> discrepancyMethods = Map.of(
                "52-week High/Low", stockDiscrepanciesRepository::findStocksWithHighLow52wDiscrepancy,
                "4-week High/Low", stockDiscrepanciesRepository::findStocksWithHighLow4wDiscrepancy,
                "Highest Lowest", stockDiscrepanciesRepository::findStocksWithHighestLowestDiscrepancy,
                "Weekly Opening", stockDiscrepanciesRepository::findStocksWithWeeklyOpeningDiscrepancy,
                "Monthly Opening", stockDiscrepanciesRepository::findStocksWithMonthlyOpeningDiscrepancy,
                "Quarterly Opening", stockDiscrepanciesRepository::findStocksWithQuarterlyOpeningDiscrepancy,
                "Yearly Opening", stockDiscrepanciesRepository::findStocksWithYearlyOpeningDiscrepancy
        );

        discrepancyMethods.forEach((name, supplier) ->
                supplier.get().stream()
                        .findFirst()
                        .ifPresent(_ -> log.warn("Found discrepancy in {}", name))
        );
    }

}