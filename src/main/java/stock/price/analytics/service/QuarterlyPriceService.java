package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.CandleOHLC;
import stock.price.analytics.model.prices.ohlc.MonthlyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.QuarterlyPriceOHLC;
import stock.price.analytics.repository.prices.PricesRepository;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuarterlyPriceService {

    private final PricesRepository pricesRepository;

    @Transactional
    public void saveAllQuarterlyPrices() {
        List<MonthlyPriceOHLC> monthlyPrices = pricesRepository.findAllMonthlyPrices();

        Map<String, Map<String, List<MonthlyPriceOHLC>>> groupedByTickerAndQuarter =
                monthlyPrices.stream()
                        .collect(Collectors.groupingBy(
                                MonthlyPriceOHLC::getTicker,
                                Collectors.groupingBy(price -> {
                                    int year = price.getStartDate().getYear();
                                    int quarter = (price.getStartDate().getMonthValue() - 1) / 3 + 1;
                                    return year + "-Q" + quarter; // Format as "YYYY-QX"
                                })
                        ));

        List<QuarterlyPriceOHLC> quarterlyPrices = new ArrayList<>();
        groupedByTickerAndQuarter.forEach(
                (_, quarterlyData) -> quarterlyData.forEach(
                        (_, prices) -> {
                            List<MonthlyPriceOHLC> monthlyPricesSortedChronologically = prices.stream().sorted(Comparator.comparing(MonthlyPriceOHLC::getStartDate)).toList();
                            quarterlyPrices.add(quarterlyPricesFrom(monthlyPricesSortedChronologically));
                        }
                )
        );

        partitionDataAndSave(quarterlyPrices, pricesRepository);
        pricesRepository.quarterlyPricesUpdatePerformance();
    }

    private QuarterlyPriceOHLC quarterlyPricesFrom(List<MonthlyPriceOHLC> monthlyPrices) {
        if (monthlyPrices.isEmpty()) return null;

        String ticker = monthlyPrices.getFirst().getTicker();
        LocalDate quarterStartDate = monthlyPrices.getFirst().getStartDate().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate quarterEndDate = monthlyPrices.getLast().getEndDate().with(TemporalAdjusters.lastDayOfMonth());

        double open = monthlyPrices.getFirst().getOpen();
        double close = monthlyPrices.getLast().getClose();
        double high = monthlyPrices.stream().mapToDouble(MonthlyPriceOHLC::getHigh).max().orElse(0);
        double low = monthlyPrices.stream().mapToDouble(MonthlyPriceOHLC::getLow).min().orElse(0);

        return new QuarterlyPriceOHLC(ticker, quarterStartDate, quarterEndDate, new CandleOHLC(open, high, low, close));
    }
}
