package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.repository.prices.PricesDiscrepanciesRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricesDiscrepanciesService {

    private final PricesDiscrepanciesRepository pricesDiscrepanciesRepository;

    private List<String> findWeeklyOpeningPriceDiscrepancies() {
        List<String> wOpeningPriceDiscrepanciesTickers = new ArrayList<>();
        List<Object[]> weeklyOpeningPriceDiscrepancies = pricesDiscrepanciesRepository.findWeeklyOpeningPriceDiscrepancies();
        if (!weeklyOpeningPriceDiscrepancies.isEmpty()) {

            weeklyOpeningPriceDiscrepancies.forEach(resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), wOpeningPriceDiscrepanciesTickers));
        }
        return wOpeningPriceDiscrepanciesTickers;
    }

    private List<String> findWeeklyHighLowPriceDiscrepancies() {
        List<String> wHighLowPriceDiscrepanciesTickers = new ArrayList<>();
        List<Object[]> weeklyHighLowPriceDiscrepancies = pricesDiscrepanciesRepository.findWeeklyHighLowPriceDiscrepancies();
        if (!weeklyHighLowPriceDiscrepancies.isEmpty()) {

            weeklyHighLowPriceDiscrepancies.forEach(resultRow -> logDiscrepancyAndAddToList(String.valueOf(resultRow[0]), String.valueOf(resultRow[1]), wHighLowPriceDiscrepanciesTickers));
        }
        return wHighLowPriceDiscrepanciesTickers;
    }

    private void logDiscrepancyAndAddToList(String ticker, String name, List<String> discrepanciesFound) {
        String stockDiscrepancy = String.format("%s with %s discrepancy", ticker, name);
        discrepanciesFound.add(stockDiscrepancy);
        log.warn("{}", stockDiscrepancy);
    }

}
