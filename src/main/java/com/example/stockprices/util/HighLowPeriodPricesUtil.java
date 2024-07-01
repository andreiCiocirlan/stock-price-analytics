package com.example.stockprices.util;

import com.example.stockprices.model.prices.StockHistoricalPrices;
import com.example.stockprices.model.prices.StockWeeklyHistoricalPrices;
import com.example.stockprices.model.prices.enums.StockPerformanceInterval;
import com.example.stockprices.model.prices.highlow.HighLow30Days;
import com.example.stockprices.model.prices.highlow.HighLow52Week;
import com.example.stockprices.model.prices.highlow.HighLowForPeriod;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class HighLowPeriodPricesUtil extends StockHistoricalPricesUtil{


    public static List<HighLowForPeriod> highLow30DaysFromFile(Path srcFile, LocalDate startDate, LocalDate endDate, StockPerformanceInterval stockPerformanceInterval) throws IOException {
        return highLow52WeeksFromFile(srcFile, startDate, endDate, stockPerformanceInterval);
    }

    public static List<HighLowForPeriod> highLow52WeeksFromFile(Path srcFile, LocalDate startDate, LocalDate endDate, StockPerformanceInterval stockPerformanceInterval) throws IOException {
        return highLowFromFileForPeriod(srcFile, startDate, endDate, stockPerformanceInterval);
    }

    private static List<HighLowForPeriod> highLowFromFileForPeriod(Path srcFile, LocalDate startDate, LocalDate endDate, StockPerformanceInterval stockPerformanceInterval) throws IOException {
        return getHighLowForPeriod(
                weeklyPricesFrom(dailyPricesFromFile(srcFile)).stream().filter(whp -> whp.getWeekStart().isAfter(startDate) && whp.getWeekStart().isBefore(endDate)).toList(),
                dailyPricesFromFile(srcFile).stream().filter(shp -> shp.getDate().isAfter(startDate) && shp.getDate().isBefore(endDate)).toList(),
                stockPerformanceInterval
        );
    }

    private static List<HighLowForPeriod> getHighLowForPeriod(List<StockWeeklyHistoricalPrices> weeklyHistoricalPrices, List<StockHistoricalPrices> dailyPrices, StockPerformanceInterval stockPerformanceInterval) {
        List<HighLowForPeriod> highLow30DaysForEachFridays = new ArrayList<>();
        for (StockWeeklyHistoricalPrices whp : weeklyHistoricalPrices) {
            double fridayClose = whp.getClose();
            String ticker = whp.getTicker();
            LocalDate week_end = whp.getWeekEnd(); // Friday
            LocalDate startDate; // going back in time for the past X days/weeks etc.
            startDate = StockPerformanceInterval.STOCK_PERF_INTERVAL_30D.equals(stockPerformanceInterval) ? week_end.minus(Period.ofDays(31))
                    : week_end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(52);
            LocalDate endDate = week_end.plusDays(1); // default include Friday prices

            HighLowForPeriod highLowForPeriod;
            if (StockPerformanceInterval.STOCK_PERF_INTERVAL_52W.equals(stockPerformanceInterval) ) {
                highLowForPeriod = new HighLow52Week(ticker, whp.getWeekStart(), week_end, fridayClose);
            } else {
                highLowForPeriod = new HighLow30Days(ticker, whp.getWeekStart(), week_end, fridayClose);
            }

            highLowForPeriod.setHigh(
                    dailyPrices.stream()
                            .filter(shp1 -> shp1.getTicker().equals(ticker))
                            .filter(p1 -> p1.getDate().isAfter(startDate) && p1.getDate().isBefore(endDate))
                            .mapToDouble(StockHistoricalPrices::getHigh)
                            .max()
                            .orElseThrow());
            highLowForPeriod.setLow(
                    dailyPrices.stream()
                            .filter(shp -> shp.getTicker().equals(ticker))
                            .filter(p -> p.getDate().isAfter(startDate) && p.getDate().isBefore(endDate))
                            .mapToDouble(StockHistoricalPrices::getLow)
                            .min()
                            .orElseThrow());

            highLow30DaysForEachFridays.add(highLowForPeriod);
        }
        return highLow30DaysForEachFridays;
    }

}