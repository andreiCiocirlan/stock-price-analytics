package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PricesUtil {

    public static List<AbstractPrice> getHigherTimeframePricesFor(List<DailyPrice> dailyPricesImported) {
        List<AbstractPrice> htfPrices = new ArrayList<>();
        for (StockTimeframe higherTimeframe : StockTimeframe.higherTimeframes()) {
            htfPrices.addAll(htfPricesForTimeframe(dailyPricesImported, higherTimeframe));
        }
        return htfPrices;
    }

    public static List<AbstractPrice> htfPricesForTimeframe(List<DailyPrice> dailyPrices, StockTimeframe stockTimeframe) {
        return pricesWithPerformance(dailyPrices.stream()
                .collect(Collectors.groupingBy(
                        shp -> groupingFunctionFor(stockTimeframe).apply(shp.getDate()) + "-" + shp.getTicker(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                prices -> extractPriceForTimeframe(prices, stockTimeframe)
                        )
                )).values().stream().sorted(Comparator.comparing(AbstractPrice::getStartDate)).toList());
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

    public static <T extends AbstractPrice> List<T> pricesWithPerformance(List<T> prices) {
        for (int i = prices.size() - 1; i >= 1; i--) {
            double previousClose = prices.get(i - 1).getClose();
            double performance = ((prices.get(i).getClose() - previousClose) / previousClose) * 100;
            prices.get(i).setPerformance(Math.round(performance * 100.0) / 100.0);
        }
        return prices;
    }

    public static AbstractPrice multiplyWith(AbstractPrice price, double priceMultiplier) {
        price.setOpen(Math.round((priceMultiplier * price.getOpen()) * 100.0) / 100.0);
        price.setHigh(Math.round((priceMultiplier * price.getHigh()) * 100.0) / 100.0);
        price.setLow(Math.round((priceMultiplier * price.getLow()) * 100.0) / 100.0);
        price.setClose(Math.round((priceMultiplier * price.getClose()) * 100.0) / 100.0);
        return price;
    }

    static DailyPrice dailyPriceWithRoundedDecimals(DailyPrice dailyPrice) {
        dailyPrice.setOpen(Double.parseDouble(String.format("%.4f", dailyPrice.getOpen())));
        dailyPrice.setHigh(Double.parseDouble(String.format("%.4f", dailyPrice.getHigh())));
        dailyPrice.setLow(Double.parseDouble(String.format("%.4f", dailyPrice.getLow())));
        dailyPrice.setClose(Double.parseDouble(String.format("%.4f", dailyPrice.getClose())));
//        if (dailyPrice.getOpen() < 1d) {
//            dailyPrice.setOpen(Math.round(dailyPrice.getOpen() * 100.0) / 100.0);
//            dailyPrice.setHigh(Math.round(dailyPrice.getHigh() * 100.0) / 100.0);
//            dailyPrice.setLow(Math.round(dailyPrice.getLow() * 100.0) / 100.0);
//            dailyPrice.setClose(Math.round(dailyPrice.getClose() * 100.0) / 100.0);
//        } else {
//            dailyPrice.setOpen(Math.round(dailyPrice.getOpen() * 1000.0) / 1000.0);
//            dailyPrice.setHigh(Math.round(dailyPrice.getHigh() * 1000.0) / 1000.0);
//            dailyPrice.setLow(Math.round(dailyPrice.getLow() * 1000.0) / 1000.0);
//            dailyPrice.setClose(Math.round(dailyPrice.getClose() * 1000.0) / 1000.0);
//        }
        return dailyPrice;
    }
}