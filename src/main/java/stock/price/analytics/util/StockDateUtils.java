package stock.price.analytics.util;

import java.time.LocalDate;

public class StockDateUtils {

    public static boolean sameWeek(LocalDate date, LocalDate startOfWeek) {
        return date.isAfter(startOfWeek.minusDays(7)) && date.isBefore(startOfWeek.plusDays(7));
    }

    public static boolean sameMonth(LocalDate date, LocalDate startOfMonth) {
        return date.getYear() == startOfMonth.getYear() && date.getMonthValue() == startOfMonth.getMonthValue();
    }

    public static boolean sameYear(LocalDate date, LocalDate startOfYear) {
        return date.getYear() == startOfYear.getYear();
    }
}