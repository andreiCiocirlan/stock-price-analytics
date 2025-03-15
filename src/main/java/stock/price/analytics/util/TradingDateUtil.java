package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.util.StockDateUtils.isWithinSameTimeframe;

public class TradingDateUtil {

    private static final ZoneId NY_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime START_MARKET_HOURS_NYSE = LocalTime.of(9, 30);
    private static final LocalTime END_MARKET_HOURS_NYSE = LocalTime.of(16, 0);

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
            return LocalDate.now(NY_ZONE).with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
        }

        if (isBetweenMarketHours() || isAfterMarketHours()) {
            return LocalDate.now(NY_ZONE);
        } else { // Before market hours
            return LocalDate.now(NY_ZONE).minusDays(1);
        }
    }

    public static boolean isFirstImportFor(StockTimeframe timeframe, LocalDate previousImportDate) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY, YEARLY, QUARTERLY, MONTHLY -> !isWithinSameTimeframe(LocalDate.now(NY_ZONE), previousImportDate, timeframe);
        };
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
