package stock.price.analytics.model.prices.enums;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

public enum StockTimeframe {

    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY;

    public String dbTablePerfHeatmap() {
        return switch (this) {
            case DAILY -> "daily_prices_performance_view";
            case WEEKLY -> "weekly_prices_performance_view";
            case MONTHLY -> "monthly_prices_performance_view";
            case QUARTERLY -> "quarterly_prices_performance_view";
            case YEARLY -> "yearly_prices_performance_view";
        };
    }

    public String dbTableOHLC() {
        return switch (this) {
            case DAILY -> "daily_prices";
            case WEEKLY -> "weekly_prices";
            case MONTHLY -> "monthly_prices";
            case QUARTERLY -> "quarterly_prices";
            case YEARLY -> "yearly_prices";
        };
    }

    public String toSQLInterval() {
        return switch (this) {
            case DAILY -> "2 DAY";
            case WEEKLY -> "2 WEEK";
            case MONTHLY -> "2 MONTH";
            case QUARTERLY -> "6 MONTH"; // quarter = 3 months
            case YEARLY -> "2 YEAR";
        };
    }

    public LocalDate htfDateFrom(LocalDate date) {
        LocalDate tradingDateNow = tradingDateNow();

        return switch (this) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> tradingDateNow.isBefore(date.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))) ? tradingDateNow : date.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
            case MONTHLY -> tradingDateNow.isBefore(date.with(lastDayOfMonth())) ? tradingDateNow : date.with(lastDayOfMonth());
            case QUARTERLY -> quarterEndDate(date, tradingDateNow);
            case YEARLY -> tradingDateNow.isBefore(date.with(lastDayOfYear())) ? tradingDateNow : date.with(lastDayOfYear());
        };
    }

    private static LocalDate quarterEndDate(LocalDate date, LocalDate tradingDateNow) {
        YearMonth yearMonth = YearMonth.from(date);
        YearMonth lastMonthOfQuarter;

        // Determine the last month of the quarter
        int month = yearMonth.getMonthValue();
        if (month <= 3) { // Q1
            lastMonthOfQuarter = YearMonth.of(yearMonth.getYear(), 3);
        } else if (month <= 6) { // Q2
            lastMonthOfQuarter = YearMonth.of(yearMonth.getYear(), 6);
        } else if (month <= 9) { // Q3
            lastMonthOfQuarter = YearMonth.of(yearMonth.getYear(), 9);
        } else { // Q4
            lastMonthOfQuarter = YearMonth.of(yearMonth.getYear(), 12);
        }

        // Get the last day of that quarter
        LocalDate lastDayOfQuarter = lastMonthOfQuarter.atEndOfMonth();

        return tradingDateNow.isBefore(lastDayOfQuarter) ? tradingDateNow : lastDayOfQuarter;
    }

    public static List<StockTimeframe> higherTimeframes() {
        return List.of(WEEKLY, MONTHLY, QUARTERLY, YEARLY);
    }

    /**
     * @param timeframeLetters any of WMY
     * @return corresponding stock timeframes from WMY -> [WEEKLY, MONTHLY, YEARLY]
     */
    public static List<StockTimeframe> valuesFromLetters(String timeframeLetters) {
        Set<StockTimeframe> timeframes = new HashSet<>();

        timeframeLetters = timeframeLetters.toUpperCase(); // sanitize for dwmy
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            if (timeframeLetters.contains(timeframe.name().substring(0, 1))) {
                timeframes.add(timeframe);
            }
        }

        return timeframes.stream().toList();
    }
}