package stock.price.analytics.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
        if (isBetweenMarketHours() || (isAfterMarketHours() && LocalDateTime.now().toLocalTime().getHour() == 23))
            return LocalDate.now();

        // is before market hours || is after 23:59
        return LocalDate.now().minusDays(1);
    }



}
