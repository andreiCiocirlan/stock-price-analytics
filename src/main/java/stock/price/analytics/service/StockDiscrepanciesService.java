package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockDiscrepanciesRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDiscrepanciesService {

    private final StockDiscrepanciesRepository stockDiscrepanciesRepository;

    public String findHighLowAndOpeningPriceDiscrepancies() {
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
                        .ifPresent(_ -> log.warn("Found discrepancies in: {}", name))
        );
        boolean discrepanciesFound = discrepancyMethods.entrySet().stream()
                .anyMatch(entry -> !entry.getValue().get().isEmpty());

        StringBuilder discrepancyMessageBuilder = new StringBuilder();
        if (discrepanciesFound) {
            discrepancyMethods.forEach((name, supplier) ->
                    supplier.get().stream()
                            .findFirst()
                            .ifPresent(_ -> discrepancyMessageBuilder.append("Found discrepancies in: ").append(name).append("\n"))
            );
            return "The following discrepancies were found:\n" + discrepancyMessageBuilder.toString().trim();
        } else {
            return "No discrepancies found.";
        }
    }

}