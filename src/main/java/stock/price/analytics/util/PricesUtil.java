package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PricesUtil {

    public static <T extends AbstractPrice> List<T> pricesWithPerformance(List<T> prices) {
        for (int i = prices.size() - 1; i >= 1; i--) {
            double previousClose = prices.get(i - 1).getClose();
            double performance = ((prices.get(i).getClose() - previousClose) / previousClose) * 100;
            prices.get(i).setPerformance(Math.round(performance * 100.0) / 100.0);
        }
        return prices;
    }

    public static List<AbstractPrice> htfPricesForTimeframe(List<DailyPrice> dailyPrices, StockTimeframe stockTimeframe) {
        return new ArrayList<>(
                dailyPrices.stream()
                        .collect(Collectors.groupingBy(
                                shp -> groupingFunctionFor(stockTimeframe).apply(shp.getDate()),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        prices -> extractPriceForTimeframe(prices, stockTimeframe)
                                )
                        )).values());
    }

    private static Function<LocalDate, ? extends Temporal> groupingFunctionFor(StockTimeframe stockTimeframe) {
        return switch (stockTimeframe) {
            case WEEKLY -> shp -> shp.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> YearMonth::from;
            case QUARTERLY -> shp -> YearMonth.of(shp.getYear(), shp.getMonth().firstMonthOfQuarter().getValue());
            case YEARLY -> Year::from;
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
        };
    }

    private static AbstractPrice extractPriceForTimeframe(List<DailyPrice> pricesGroupedByTimeFrame, StockTimeframe stockTimeframe) {
        DailyPrice firstInChronologicalOrder = pricesGroupedByTimeFrame.getFirst(); // already sorted
        DailyPrice lastInChronologicalOrder = pricesGroupedByTimeFrame.getLast();
        String ticker = firstInChronologicalOrder.getTicker();
        LocalDate startDate = firstInChronologicalOrder.getDate();
        double open = firstInChronologicalOrder.getOpen();
        double close = lastInChronologicalOrder.getClose();
        double high = pricesGroupedByTimeFrame.stream()
                .mapToDouble(DailyPrice::getHigh)
                .max()
                .orElseThrow();
        double low = pricesGroupedByTimeFrame.stream()
                .mapToDouble(DailyPrice::getLow)
                .min()
                .orElseThrow();

        CandleOHLC candleOHLC = new CandleOHLC(open, high, low, close);
        return switch (stockTimeframe) {
            case WEEKLY -> new WeeklyPrice(ticker, startDate, candleOHLC);
            case MONTHLY -> new MonthlyPrice(ticker, startDate, candleOHLC);
            case QUARTERLY -> new QuarterlyPrice(ticker, startDate, candleOHLC);
            case YEARLY -> new YearlyPrice(ticker, startDate, candleOHLC);
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
        };
    }

}