package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class DateUtil {

    public static LocalDate convertDateToTimeframe(LocalDate date, StockTimeframe timeframe) {
        switch (timeframe) {
            case DAILY:
                return date;
            case WEEKLY:
                // Convert date to start of week (e.g., Monday)
                return date.with(DayOfWeek.MONDAY);
            case MONTHLY:
                // Convert date to first day of month
                return date.withDayOfMonth(1);
            case QUARTERLY:
                // Convert to first day of quarter
                int currentQuarter = (date.getMonthValue() - 1) / 3 + 1;
                int firstMonthOfQuarter = (currentQuarter - 1) * 3 + 1;
                return LocalDate.of(date.getYear(), firstMonthOfQuarter, 1);
            case YEARLY:
                // Convert to first day of year
                return LocalDate.of(date.getYear(), 1, 1);
            default:
                throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        }
    }

}
