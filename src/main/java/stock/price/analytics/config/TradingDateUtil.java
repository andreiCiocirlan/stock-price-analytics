package stock.price.analytics.config;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class TradingDateUtil {

    private static final LocalTime startMarketHours = LocalTime.of(16, 30, 0, 0);
    private static final LocalTime endMarketHours = LocalTime.of(23, 0, 0, 0);

    private static boolean isBetweenMarketHours() {
        return LocalDateTime.now().toLocalTime().isAfter(startMarketHours) && LocalDateTime.now().toLocalTime().isBefore(endMarketHours);
    }

    private static boolean isBeforeMarketHours() {
        return LocalDateTime.now().toLocalTime().isBefore(startMarketHours);
    }

    private static boolean isAfterMarketHours() {
        return LocalDateTime.now().toLocalTime().isAfter(endMarketHours);
    }

    public static LocalDate tradingDateNow() {
        if (LocalDate.now().getDayOfWeek().equals(DayOfWeek.SATURDAY) || LocalDate.now().getDayOfWeek().equals(DayOfWeek.SUNDAY)
                || (LocalDate.now().getDayOfWeek().equals(DayOfWeek.MONDAY) && isBeforeMarketHours())) {
            return LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
        }
        if (isBetweenMarketHours() || (isAfterMarketHours() && LocalDateTime.now().toLocalTime().getHour() == 23))
            return LocalDate.now();

        // is before market hours || is after 23:59
        return LocalDate.now().minusDays(1);
    }

    public static LocalDate previousTradingDate() {
        LocalDate now = LocalDate.now();
        return switch (now.getDayOfWeek()) {
            case MONDAY -> now.with(TemporalAdjusters.previous(DayOfWeek.THURSDAY));
            case TUESDAY -> now.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
            case WEDNESDAY -> now.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            case THURSDAY -> now.with(TemporalAdjusters.previous(DayOfWeek.TUESDAY));
            case FRIDAY -> now.with(TemporalAdjusters.previous(DayOfWeek.WEDNESDAY));
            case SATURDAY -> now.with(TemporalAdjusters.previous(DayOfWeek.THURSDAY));
            case SUNDAY -> now.with(TemporalAdjusters.previous(DayOfWeek.THURSDAY));
        };
    }

}
