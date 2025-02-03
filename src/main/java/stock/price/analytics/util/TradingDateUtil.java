package stock.price.analytics.util;

import stock.price.analytics.model.prices.ohlc.AbstractPrice;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static LocalDate previousTradingDate(LocalDate date) {
        if (date.getDayOfWeek().equals(DayOfWeek.SATURDAY) || date.getDayOfWeek().equals(DayOfWeek.SUNDAY)
                || (date.getDayOfWeek().equals(DayOfWeek.MONDAY))) {
            return date.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
        }

        return date.minusDays(1);
    }

    // returns the trading date being imported (highest count, as for some tickers it might import previous days)
    public static LocalDate tradingDateImported(List<? extends AbstractPrice> importedPrices) {
        return importedPrices.stream()
                .collect(Collectors.groupingBy(AbstractPrice::getStartDate, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow();
    }
}
