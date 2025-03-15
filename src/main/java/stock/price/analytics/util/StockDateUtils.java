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

    private static boolean sameWeek(LocalDate firstDate, LocalDate secondDate) {
        // Find the start of the week (Monday)
        LocalDate weekStart = secondDate.with(DayOfWeek.MONDAY);

        // Calculate the end of the week (Friday)
        LocalDate weekEnd = weekStart.plusDays(4); // 4 days after Monday is Friday

        // Check if the date is within the range of the week
        return !firstDate.isBefore(weekStart) && !firstDate.isAfter(weekEnd);
    }

    private static boolean sameMonth(LocalDate firstDate, LocalDate secondDate) {
        return firstDate.getYear() == secondDate.getYear() && firstDate.getMonthValue() == secondDate.getMonthValue();
    }

    private static boolean sameQuarter(LocalDate firstDate, LocalDate secondDate) {
        return firstDate.getYear() == secondDate.getYear() &&
                firstDate.getMonth().firstMonthOfQuarter() == secondDate.getMonth().firstMonthOfQuarter();
    }

    private static boolean sameYear(LocalDate firstDate, LocalDate secondDate) {
        return firstDate.getYear() == secondDate.getYear();
    }
}