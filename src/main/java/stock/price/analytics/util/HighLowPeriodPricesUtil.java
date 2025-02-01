package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockPerformanceInterval;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.prices.ohlc.WeeklyPrice;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class HighLowPeriodPricesUtil extends PricesUtil {

    public static List<HighLowForPeriod> highLowFromFileForPeriod(Path srcFile, LocalDate startDate, LocalDate endDate, StockPerformanceInterval stockPerformanceInterval) {
        List<DailyPrice> dailyPrices = dailyPricesFromFile(srcFile);
        List<WeeklyPrice> weeklyGroupedPrices = getPricesForTimeframe(dailyPrices, StockTimeframe.WEEKLY).stream().map(WeeklyPrice.class::cast).toList();
        return getHighLowForPeriod(
                weeklyGroupedPrices.stream().filter(whp -> whp.getStartDate().isAfter(startDate) && whp.getStartDate().isBefore(endDate)).toList(),
                dailyPrices.stream().filter(shp -> shp.getDate().isAfter(startDate) && shp.getDate().isBefore(endDate)).toList(),
                stockPerformanceInterval
        );
    }

    private static List<HighLowForPeriod> getHighLowForPeriod(List<WeeklyPrice> weeklyHistoricalPrices, List<DailyPrice> dailyPrices, StockPerformanceInterval stockPerformanceInterval) {
        List<HighLowForPeriod> highLowsForPeriod = new ArrayList<>();
        for (WeeklyPrice wp : weeklyHistoricalPrices) {
            String ticker = wp.getTicker();
            LocalDate week_end = wp.getEndDate(); // Friday
            LocalDate startDate; // going back in time for the past X days/weeks etc.
            startDate = StockPerformanceInterval.STOCK_PERF_INTERVAL_30D.equals(stockPerformanceInterval) ? week_end.minus(Period.ofDays(31))
                    : week_end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(52);
            LocalDate endDate = week_end.plusDays(1); // default include Friday prices

            HighLowForPeriod highLowForPeriod;
            if (StockPerformanceInterval.STOCK_PERF_INTERVAL_52W.equals(stockPerformanceInterval)) {
                highLowForPeriod = new HighLow52Week(ticker, wp.getStartDate(), week_end);
            } else {
                highLowForPeriod = new HighLow4w(ticker, wp.getStartDate(), week_end);
            }

            highLowForPeriod.setHigh(dailyPrices.stream()
                    .filter(shp1 -> shp1.getTicker().equals(ticker))
                    .filter(p1 -> p1.getDate().isAfter(startDate) && p1.getDate().isBefore(endDate))
                    .mapToDouble(DailyPrice::getHigh)
                    .max()
                    .orElseThrow());
            highLowForPeriod.setLow(dailyPrices.stream()
                    .filter(shp -> shp.getTicker().equals(ticker))
                    .filter(p -> p.getDate().isAfter(startDate) && p.getDate().isBefore(endDate))
                    .mapToDouble(DailyPrice::getLow)
                    .min()
                    .orElseThrow());

            highLowsForPeriod.add(highLowForPeriod);
        }
        return highLowsForPeriod;
    }

}