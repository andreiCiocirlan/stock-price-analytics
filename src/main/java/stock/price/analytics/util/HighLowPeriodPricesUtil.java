package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockPerformanceInterval;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.WeeklyPriceOHLC;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class HighLowPeriodPricesUtil extends PricesOHLCUtil {

    public static List<HighLowForPeriod> highLowFromFileForPeriod(Path srcFile, LocalDate startDate, LocalDate endDate, StockPerformanceInterval stockPerformanceInterval) throws IOException {
        List<DailyPriceOHLC> dailyPrices = dailyPricesOHLCFromFile(srcFile);
        List<WeeklyPriceOHLC> weeklyGroupedPrices = getPriceOHLCsForTimeframe(dailyPrices, StockTimeframe.WEEKLY).stream().map(WeeklyPriceOHLC.class::cast).toList();
        return getHighLowForPeriod(
                weeklyGroupedPrices.stream().filter(whp -> whp.getStartDate().isAfter(startDate) && whp.getStartDate().isBefore(endDate)).toList(),
                dailyPrices.stream().filter(shp -> shp.getDate().isAfter(startDate) && shp.getDate().isBefore(endDate)).toList(),
                stockPerformanceInterval
        );
    }

    private static List<HighLowForPeriod> getHighLowForPeriod(List<WeeklyPriceOHLC> weeklyHistoricalPrices, List<DailyPriceOHLC> dailyPrices, StockPerformanceInterval stockPerformanceInterval) {
        List<HighLowForPeriod> highLowsForPeriod = new ArrayList<>();
        for (WeeklyPriceOHLC wp : weeklyHistoricalPrices) {
            double fridayClose = wp.getClose();
            String ticker = wp.getTicker();
            LocalDate week_end = wp.getEndDate(); // Friday
            LocalDate startDate; // going back in time for the past X days/weeks etc.
            startDate = StockPerformanceInterval.STOCK_PERF_INTERVAL_30D.equals(stockPerformanceInterval) ? week_end.minus(Period.ofDays(31))
                    : week_end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(52);
            LocalDate endDate = week_end.plusDays(1); // default include Friday prices

            HighLowForPeriod highLowForPeriod;
            if (StockPerformanceInterval.STOCK_PERF_INTERVAL_52W.equals(stockPerformanceInterval)) {
                highLowForPeriod = new HighLow52Week(ticker, wp.getStartDate(), week_end, fridayClose);
            } else {
                highLowForPeriod = new HighLow4w(ticker, wp.getStartDate(), week_end, fridayClose);
            }

            highLowForPeriod.setHigh(dailyPrices.stream()
                    .filter(shp1 -> shp1.getTicker().equals(ticker))
                    .filter(p1 -> p1.getDate().isAfter(startDate) && p1.getDate().isBefore(endDate))
                    .mapToDouble(DailyPriceOHLC::getHigh)
                    .max()
                    .orElseThrow());
            highLowForPeriod.setLow(dailyPrices.stream()
                    .filter(shp -> shp.getTicker().equals(ticker))
                    .filter(p -> p.getDate().isAfter(startDate) && p.getDate().isBefore(endDate))
                    .mapToDouble(DailyPriceOHLC::getLow)
                    .min()
                    .orElseThrow());

            highLowsForPeriod.add(highLowForPeriod);
        }
        return highLowsForPeriod;
    }

}