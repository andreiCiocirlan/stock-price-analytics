package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PricesUtil {

    public static Map<StockTimeframe, List<AbstractPrice>> getHigherTimeframePricesMapFor(List<DailyPrice> dailyPricesImported) {
        Map<StockTimeframe, List<AbstractPrice>> timeframeToPrices = new HashMap<>();
        for (StockTimeframe higherTimeframe : StockTimeframe.higherTimeframes()) {
            timeframeToPrices.put(higherTimeframe, htfPricesForTimeframe(dailyPricesImported, higherTimeframe));
        }
        return timeframeToPrices;
    }

    private static List<AbstractPrice> htfPricesForTimeframe(List<DailyPrice> dailyPrices, StockTimeframe stockTimeframe) {
        List<AbstractPrice> result = new ArrayList<>();

        Map<String, List<DailyPrice>> pricesByTicker = dailyPrices.stream().collect(Collectors.groupingBy(DailyPrice::getTicker));

        for (List<DailyPrice> tickerPrices : pricesByTicker.values()) {
            List<AbstractPrice> htfPrices =
                    tickerPrices.stream()
                            .collect(Collectors.groupingBy(
                                    price -> groupingFunctionFor(stockTimeframe).apply(price.getDate()),
                                    Collectors.collectingAndThen(
                                            Collectors.toList(),
                                            prices -> extractPriceForTimeframe(prices, stockTimeframe)
                                    )
                            ))
                            .values()
                            .stream()
                            .sorted(Comparator.comparing(AbstractPrice::getDate))
                            .collect(Collectors.toList());

            pricesWithPerformance(htfPrices);

            result.addAll(htfPrices);
        }

        return result;
    }

    private static Function<LocalDate, Temporal> groupingFunctionFor(StockTimeframe stockTimeframe) {
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
        LocalDate date = firstInChronologicalOrder.getDate();
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
            case WEEKLY -> new WeeklyPrice(ticker, date, candleOHLC);
            case MONTHLY -> new MonthlyPrice(ticker, date, candleOHLC);
            case QUARTERLY -> new QuarterlyPrice(ticker, date, candleOHLC);
            case YEARLY -> new YearlyPrice(ticker, date, candleOHLC);
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
        };
    }

    public static <T extends AbstractPrice> List<T> pricesWithPerformance(List<T> prices) {
        if (prices.isEmpty()) {
            return prices;
        }

        prices.getFirst().setPerformance(0);

        for (int i = 1; i < prices.size(); i++) {
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