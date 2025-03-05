package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class StockDateUtils {

    public static boolean isWithinSameTimeframe(LocalDate date, LocalDate latestEndDateWMY, StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> sameWeek(date, latestEndDateWMY);
            case MONTHLY -> sameMonth(date, latestEndDateWMY);
            case QUARTERLY -> sameQuarter(date, latestEndDateWMY);
            case YEARLY -> sameYear(date, latestEndDateWMY);
        };
    }

    public static boolean sameWeek(LocalDate date, LocalDate endDate) {
        // Find the start of the week (Monday)
        LocalDate weekStart = endDate.with(DayOfWeek.MONDAY);

        // Calculate the end of the week (Friday)
        LocalDate weekEnd = weekStart.plusDays(4); // 4 days after Monday is Friday

        // Check if the date is within the range of the week
        return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
    }

    public static boolean sameMonth(LocalDate date, LocalDate startOfMonth) {
        return date.getYear() == startOfMonth.getYear() && date.getMonthValue() == startOfMonth.getMonthValue();
    }

    public static boolean sameQuarter(LocalDate date, LocalDate latestEndDateQuarter) {
        return date.getYear() == latestEndDateQuarter.getYear() &&
                date.getMonth().firstMonthOfQuarter() == latestEndDateQuarter.getMonth().firstMonthOfQuarter();
    }

    public static boolean sameYear(LocalDate date, LocalDate startOfYear) {
        return date.getYear() == startOfYear.getYear();
    }
}