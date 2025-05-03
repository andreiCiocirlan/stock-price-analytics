package stock.price.analytics.util;

import stock.price.analytics.model.json.DailyPriceJSON;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

import static stock.price.analytics.util.Constants.*;

public final class TradingDateUtil {

    private static boolean isBetweenMarketHours() {
        LocalTime localTimeNowInNY = localTimeNowInNY();
        return localTimeNowInNY.isAfter(START_MARKET_HOURS_NYSE) && localTimeNowInNY.isBefore(END_MARKET_HOURS_NYSE);
    }

    private static boolean isBeforeMarketHours() {
        return localTimeNowInNY().isBefore(START_MARKET_HOURS_NYSE);
    }

    private static boolean isAfterMarketHours() {
        return localTimeNowInNY().isAfter(END_MARKET_HOURS_NYSE);
    }

    public static LocalTime localTimeNowInNY() {
        return LocalTime.now(NY_ZONE);
    }

    public static LocalDate dateNowInNY() {
        return LocalDate.now(NY_ZONE);
    }

    public static LocalDate tradingDateNow() {
        LocalDate nowInNewYork = dateNowInNY();
        DayOfWeek dayOfWeekInNY = nowInNewYork.getDayOfWeek();

        if (dayOfWeekInNY.equals(DayOfWeek.SATURDAY) || dayOfWeekInNY.equals(DayOfWeek.SUNDAY)
            || (dayOfWeekInNY.equals(DayOfWeek.FRIDAY) && isBetweenMarketHours())
            || (dayOfWeekInNY.equals(DayOfWeek.MONDAY) && isBeforeMarketHours())) {
            return nowInNewYork.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        }

        if (isBetweenMarketHours() || isAfterMarketHours()) {
            return nowInNewYork;
        } else { // Before market hours
            return nowInNewYork.minusDays(1);
        }
    }

    // returns the trading date being imported
    public static LocalDate tradingDateImported(List<DailyPriceJSON> importedPrices) {
        return importedPrices.stream()
                .max(Comparator.comparing(DailyPriceJSON::getDate))
                .map(DailyPriceJSON::getDate)
                .orElseThrow();
    }

    public static boolean isWithinSameTimeframe(LocalDate date, LocalDate latestEndDateWMY, StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> date.isEqual(latestEndDateWMY);
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
