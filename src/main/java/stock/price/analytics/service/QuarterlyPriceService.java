package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.CandleOHLC;
import stock.price.analytics.model.prices.ohlc.MonthlyPrice;
import stock.price.analytics.model.prices.ohlc.QuarterlyPrice;
import stock.price.analytics.repository.prices.QuarterlyPricesRepository;

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
public class QuarterlyPriceService {

    private final QuarterlyPricesRepository quarterlyPricesRepository;

    @Transactional
    public void saveAllQuarterlyPrices() {
        List<MonthlyPrice> monthlyPrices = quarterlyPricesRepository.findAllMonthlyPrices();

        Map<String, Map<String, List<MonthlyPrice>>> monthlyPricesByTickerAndQuarter =
                monthlyPrices.stream()
                        .collect(Collectors.groupingBy(
                                MonthlyPrice::getTicker,
                                Collectors.groupingBy(QuarterlyPriceService::compositeKeyFrom)
                        ));

        List<QuarterlyPrice> quarterlyPrices = new ArrayList<>();
        monthlyPricesByTickerAndQuarter.forEach(
                (_, quarterlyData) -> quarterlyData.forEach(
                        (_, prices) -> {
                            List<MonthlyPrice> monthlyPricesSortedChronologically = prices.stream().sorted(Comparator.comparing(MonthlyPrice::getStartDate)).toList();
                            quarterlyPrices.add(quarterlyPricesFrom(monthlyPricesSortedChronologically));
                        }
                )
        );

        partitionDataAndSave(quarterlyPrices, quarterlyPricesRepository);
        quarterlyPricesRepository.quarterlyPricesUpdatePerformance();
    }

    private static String compositeKeyFrom(MonthlyPrice price) {
        int year = price.getStartDate().getYear();
        int quarter = (price.getStartDate().getMonthValue() - 1) / 3 + 1;
        return year + "-Q" + quarter;
    }

    private QuarterlyPrice quarterlyPricesFrom(List<MonthlyPrice> monthlyPrices) {
        if (monthlyPrices.isEmpty()) return null;

        String ticker = monthlyPrices.getFirst().getTicker();
        LocalDate quarterStartDate = monthlyPrices.getFirst().getStartDate().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate quarterEndDate = monthlyPrices.getLast().getEndDate().with(TemporalAdjusters.lastDayOfMonth());

        double open = monthlyPrices.getFirst().getOpen();
        double close = monthlyPrices.getLast().getClose();
        double high = monthlyPrices.stream().mapToDouble(MonthlyPrice::getHigh).max().orElse(0);
        double low = monthlyPrices.stream().mapToDouble(MonthlyPrice::getLow).min().orElse(0);

        return new QuarterlyPrice(ticker, quarterStartDate, quarterEndDate, new CandleOHLC(open, high, low, close));
    }
}
