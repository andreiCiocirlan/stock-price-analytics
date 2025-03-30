package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.util.Constants.*;

public class TradingDateUtil {

    private static boolean isBetweenMarketHours() {
        LocalTime nowInNY = LocalDateTime.now(NY_ZONE).toLocalTime();
        return nowInNY.isAfter(START_MARKET_HOURS_NYSE) && nowInNY.isBefore(END_MARKET_HOURS_NYSE);
    }

    private static boolean isBeforeMarketHours() {
        LocalTime nowInNY = LocalDateTime.now(NY_ZONE).toLocalTime();
        return nowInNY.isBefore(START_MARKET_HOURS_NYSE);
    }

    private static boolean isAfterMarketHours() {
        LocalTime nowInNY = LocalDateTime.now(NY_ZONE).toLocalTime();
        return nowInNY.isAfter(END_MARKET_HOURS_NYSE);
    }

    public static LocalDate tradingDateNow() {
        DayOfWeek dayOfWeekInNY = LocalDate.now(NY_ZONE).getDayOfWeek();

        if (dayOfWeekInNY.equals(DayOfWeek.SATURDAY) || dayOfWeekInNY.equals(DayOfWeek.SUNDAY)
            || (dayOfWeekInNY.equals(DayOfWeek.FRIDAY) && isBetweenMarketHours())
            || (dayOfWeekInNY.equals(DayOfWeek.MONDAY) && isBeforeMarketHours())) {
            return LocalDate.now(NY_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        }

        if (isBetweenMarketHours() || isAfterMarketHours()) {
            return LocalDate.now(NY_ZONE);
        } else { // Before market hours
            return LocalDate.now(NY_ZONE).minusDays(1);
        }
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
        return firstDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).isEqual(secondDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
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
