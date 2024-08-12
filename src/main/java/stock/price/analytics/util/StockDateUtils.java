package stock.price.analytics.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class StockDateUtils {

    public static boolean sameWeek(LocalDate date, LocalDate endDate) {
        // Find the start of the week (Monday)
        LocalDate weekStart = endDate.with(DayOfWeek.MONDAY);

        // Calculate the end of the week (Friday)
        LocalDate weekEnd = weekStart.plusDays(4); // 4 days after Monday is Friday

        // Check if the date is within the range of the week
        return !date.isBefore(weekStart) && !date.isAfter(weekEnd);    }

    public static boolean sameMonth(LocalDate date, LocalDate startOfMonth) {
        return date.getYear() == startOfMonth.getYear() && date.getMonthValue() == startOfMonth.getMonthValue();
    }

    public static boolean sameYear(LocalDate date, LocalDate startOfYear) {
        return date.getYear() == startOfYear.getYear();
    }
}